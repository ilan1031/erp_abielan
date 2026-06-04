package com.example.data

import kotlinx.coroutines.flow.Flow

class DocumentRepository(
    private val dao: BusinessDocumentDao,
    private val itemDao: ErpItemDao
) {
    val allDocuments: Flow<List<BusinessDocument>> = dao.getAllDocuments()
    val allRecurringSOs: Flow<List<RecurringSO>> = dao.getAllRecurringSOs()
    val allItems: Flow<List<ErpItem>> = itemDao.getAllItems()

    fun getDocumentsByType(type: String): Flow<List<BusinessDocument>> = dao.getDocumentsByType(type)
    fun getDocumentByIdFlow(id: Int): Flow<BusinessDocument?> = dao.getDocumentByIdFlow(id)
    suspend fun getDocumentById(id: Int): BusinessDocument? = dao.getDocumentById(id)

    suspend fun insertDocument(doc: BusinessDocument): Long = dao.insertDocument(doc)
    suspend fun updateDocument(doc: BusinessDocument) = dao.updateDocument(doc)
    suspend fun deleteDocument(doc: BusinessDocument) = dao.deleteDocument(doc)

    suspend fun getRecurringSOById(id: Int): RecurringSO? = dao.getRecurringSOById(id)
    suspend fun insertRecurringSO(so: RecurringSO): Long = dao.insertRecurringSO(so)
    suspend fun updateRecurringSO(so: RecurringSO) = dao.updateRecurringSO(so)
    suspend fun deleteRecurringSO(so: RecurringSO) = dao.deleteRecurringSO(so)

    suspend fun insertItem(item: ErpItem): Long = itemDao.insertItem(item)
    suspend fun updateItem(item: ErpItem) = itemDao.updateItem(item)
    suspend fun deleteItem(item: ErpItem) = itemDao.deleteItem(item)
}
