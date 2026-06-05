package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DocumentRepository(
    private val dao: BusinessDocumentDao,
    private val itemDao: ErpItemDao,
    private val partnerDao: PartnerDao? = null
) {
    val allDocuments: Flow<List<BusinessDocument>> = dao.getAllDocuments()
    val allRecurringSOs: Flow<List<RecurringSO>> = dao.getAllRecurringSOs()
    val allItems: Flow<List<ErpItem>> = itemDao.getAllItems()
    val allPartners: Flow<List<Partner>> = partnerDao?.getAllPartners() ?: flowOf(emptyList())

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

    suspend fun insertPartner(partner: Partner): Long = partnerDao?.insertPartner(partner) ?: -1L
    suspend fun updatePartner(partner: Partner) { partnerDao?.updatePartner(partner) }
    suspend fun deletePartner(partner: Partner) { partnerDao?.deletePartner(partner) }
    suspend fun getPartnerById(id: Int): Partner? = partnerDao?.getPartnerById(id)
}
