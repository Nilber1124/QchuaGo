package com.proyect.qchuago.data.servicio

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.entidad.Leccion
import com.proyect.qchuago.data.entidad.Nivel
import com.proyect.qchuago.data.entidad.Unidad

class CursoService {
    private val firestore = FirebaseFirestore.getInstance()
    private val nivelesCollection = firestore.collection("niveles")
    private val unidadesCollection = firestore.collection("unidades")

    suspend fun obtenerNiveles(): Result<List<Nivel>> = try {
        val snapshot = esperar(
            nivelesCollection.orderBy("orden", Query.Direction.ASCENDING).get()
        )
        val niveles = snapshot.documents.mapNotNull { documento ->
            documento.toObject(Nivel::class.java)
        }.filter { it.activo }
        Result.success(niveles)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerUnidades(nivelId: String): Result<List<Unidad>> = try {
        val snapshotUnidades = esperar(
            unidadesCollection.whereEqualTo("nivelId", nivelId).get()
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
        }.sortedBy { it.orden }
        Result.success(unidades)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerActividades(unidadId: String, leccionId: String): Result<List<Actividad>> = try {
        val snapshot = esperar(
            unidadesCollection.document(unidadId)
                .collection("lecciones").document(leccionId)
                .collection("actividades")
                .orderBy("orden", Query.Direction.ASCENDING)
                .get()
        )
        val actividades = snapshot.toObjects(Actividad::class.java).filter { it.activo }
        Result.success(actividades)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
