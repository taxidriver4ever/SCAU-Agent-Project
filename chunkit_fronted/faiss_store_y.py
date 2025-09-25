import faiss
import numpy as np
import json
import os
from typing import List, Dict, Any, Optional

class FAISSVectorStore:
    """FAISS向量存储类"""
    
    def __init__(self, index_path: str = "./faiss_index1", collection_name: str = "document_embeddings",
                 dimension: int = 1024, reset: bool = False):
        """
        初始化FAISS向量存储
        
        Args:
            index_path: 索引文件保存路径
            collection_name: 集合名称
            dimension: 向量维度
            reset: 是否重置索引
        """
        self.index_path = index_path
        self.collection_name = collection_name
        self.dimension = dimension
        
        # 创建索引目录
        os.makedirs(index_path, exist_ok=True)
        
        # 索引文件路径
        self.index_file = os.path.join(index_path, f"{collection_name}.index")
        self.metadata_file = os.path.join(index_path, f"{collection_name}_metadata.json")
        
        # 初始化索引和元数据
        if reset or not os.path.exists(self.index_file):
            self._create_new_index()
        else:
            self._load_existing_index()
    
    def _create_new_index(self):
        """创建新的FAISS索引"""
        # 使用L2距离的平面索引
        self.index = faiss.IndexFlatL2(self.dimension)
        self.metadata = {}
        self.id_to_idx = {}
        self.idx_to_id = {}
        self.next_idx = 0
        print(f"创建新的FAISS索引，维度: {self.dimension}")
    
    def _load_existing_index(self):
        """加载现有的FAISS索引"""
        try:
            # 加载索引
            self.index = faiss.read_index(self.index_file)
            
            # 加载元数据
            if os.path.exists(self.metadata_file):
                with open(self.metadata_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    self.metadata = data.get('metadata', {})
                    self.id_to_idx = data.get('id_to_idx', {})
                    self.idx_to_id = {str(v): k for k, v in self.id_to_idx.items()}
                    self.next_idx = data.get('next_idx', 0)
            else:
                self.metadata = {}
                self.id_to_idx = {}
                self.idx_to_id = {}
                self.next_idx = 0
            
            print(f"加载现有FAISS索引，当前文档数量: {self.index.ntotal}")
        except Exception as e:
            print(f"加载索引失败，创建新索引: {str(e)}")
            self._create_new_index()
    
    def add(self, documents: List[str], embeddings: List[List[float]], ids: List[str], 
            metadatas: Optional[List[Dict[str, Any]]] = None):
        """添加文档到索引"""
        if len(documents) != len(embeddings) or len(documents) != len(ids):
            raise ValueError("documents, embeddings, ids的长度必须相同")
        
        if metadatas and len(metadatas) != len(documents):
            raise ValueError("metadatas长度必须与documents相同")
        
        # 转换嵌入向量为numpy数组
        embeddings_array = np.array(embeddings, dtype=np.float32)
        
        # 添加到FAISS索引
        start_idx = self.next_idx
        self.index.add(embeddings_array)
        
        # 更新映射和元数据
        for i, (doc_id, document) in enumerate(zip(ids, documents)):
            idx = start_idx + i
            self.id_to_idx[doc_id] = idx
            self.idx_to_id[str(idx)] = doc_id
            
            # 存储文档内容和元数据
            doc_metadata = {
                'content': document,
                'id': doc_id
            }
            if metadatas and i < len(metadatas):
                doc_metadata.update(metadatas[i])
            
            self.metadata[doc_id] = doc_metadata
        
        self.next_idx += len(documents)
        
        # 保存索引和元数据
        self.save()
    
    def search(self, query_embedding: List[float], top_k: int = 5) -> List[Dict[str, Any]]:
        """搜索相似文档"""
        if self.index.ntotal == 0:
            return []
        
        # 转换查询向量，把列表变成 NumPy 数组，并包装成二维数组形状
        query_vector = np.array([query_embedding], dtype=np.float32)
        
        # 搜索
        distances, indices = self.index.search(query_vector, min(top_k, self.index.ntotal))
        
        results = []
        for distance, idx in zip(distances[0], indices[0]):
            if idx == -1:  # FAISS返回-1表示无效结果
                continue
            
            doc_id = self.idx_to_id.get(str(idx))
            if doc_id and doc_id in self.metadata:
                result = self.metadata[doc_id].copy()
                result['distance'] = float(distance)
                result['score'] = 1.0 / (1.0 + distance)  # 转换为相似度分数
                results.append(result)
        
        return results
    
    def get(self, doc_id: str) -> Optional[Dict[str, Any]]:
        """根据ID获取文档"""
        return self.metadata.get(doc_id)
    
    def delete(self, doc_ids: List[str]):
        """删除文档（注意：FAISS不支持真正的删除，这里只是从元数据中移除）"""
        for doc_id in doc_ids:
            if doc_id in self.metadata:
                del self.metadata[doc_id]
            if doc_id in self.id_to_idx:
                idx = self.id_to_idx[doc_id]
                del self.id_to_idx[doc_id]
                if str(idx) in self.idx_to_id:
                    del self.idx_to_id[str(idx)]
        
        # 保存更新后的元数据
        self.save()
    
    def count(self) -> int:
        """返回文档数量"""
        return len(self.metadata)
    
    def save(self):
        """保存索引和元数据到磁盘"""
        # 保存FAISS索引
        faiss.write_index(self.index, self.index_file)
        
        # 保存元数据
        metadata_to_save = {
            'metadata': self.metadata,
            'id_to_idx': self.id_to_idx,
            'next_idx': self.next_idx
        }
        
        with open(self.metadata_file, 'w', encoding='utf-8') as f:
            json.dump(metadata_to_save, f, ensure_ascii=False, indent=2)
    
    def clear(self):
        """清空所有数据"""
        self._create_new_index()
        self.save()
    
    def remove_by_id_prefix(self, prefix: str):
        """根据ID前缀删除文档"""
        ids_to_remove = [doc_id for doc_id in self.metadata.keys() if doc_id.startswith(prefix)]
        if ids_to_remove:
            self.delete(ids_to_remove)
            print(f"删除了 {len(ids_to_remove)} 个以 '{prefix}' 开头的文档")
    
    @property
    def ids(self):
        """返回所有文档ID列表"""
        return list(self.metadata.keys())