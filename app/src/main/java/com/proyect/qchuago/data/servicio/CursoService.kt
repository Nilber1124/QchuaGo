package com.proyect.qchuago.data.servicio

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyect.qchuago.data.entidad.Leccion
import com.proyect.qchuago.data.entidad.Unidad

class CursoService {
    private val firestore = FirebaseFirestore.getInstance()
    private val unidadesCollection = firestore.collection("unidades")

    suspend fun obtenerUnidades(): Result<List<Unidad>> = try {
        val snapshotUnidades = esperar(
            unidadesCollection.orderBy("orden", Query.Direction.ASCENDING).get()
        )
        val unidades = snapshotUnidades.documents.map { docUnidad ->
            val unidad = docUnidad.toObject(Unidad::class.java)
                ?: Unidad(id = docUnidad.id)

            val snapshotLecciones = esperar(
                docUnidad.reference
                    .collection("lecciones")
                    .orderBy("orden", Query.Direction.ASCENDING)
                    .get()
            )
            val lecciones = snapshotLecciones.toObjects(Leccion::class.java)
            unidad.copy(lecciones = lecciones)
        }
        Result.success(unidades)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
