package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Datos de un usuario en el ranking semanal.
 */
data class UsuarioRanking(
    val uid: String = "",
    val nombre: String = "",
    val xpSemanal: Int = 0,
    val posicion: Int = 0,
)

/**
 * ViewModel para el ranking semanal (RF20).
 * Consulta la colección rankingsSemanales/{semanaId}/usuarios ordenada por xpSemanal.
 */
class RankingViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _ranking = MutableStateFlow<Recurso<List<UsuarioRanking>>>(Recurso.Cargando)
    val ranking: StateFlow<Recurso<List<UsuarioRanking>>> = _ranking.asStateFlow()

    private val _miRankingFueraDeTop = MutableStateFlow<UsuarioRanking?>(null)
    val miRankingFueraDeTop: StateFlow<UsuarioRanking?> = _miRankingFueraDeTop.asStateFlow()

    init {
        cargarRanking()
    }

    fun cargarRanking(uidUsuarioActual: String? = null) {
        _ranking.value = Recurso.Cargando
        _miRankingFueraDeTop.value = null
        viewModelScope.launch {
            try {
                val semanaId = obtenerSemanaActual()

                val snapshot = firestore.collection("rankingsSemanales")
                    .document(semanaId)
                    .collection("usuarios")
                    .orderBy("xpSemanal", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()

                val lista = snapshot.documents.mapIndexedNotNull { index, doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val nombre = doc.getString("nombre") ?: "Estudiante"
                    val xp = doc.getLong("xpSemanal")?.toInt() ?: 0
                    UsuarioRanking(uid = uid, nombre = nombre, xpSemanal = xp, posicion = index + 1)
                }

                _ranking.value = Recurso.Exito(lista)

                if (uidUsuarioActual != null && lista.none { it.uid == uidUsuarioActual }) {
                    val miDoc = firestore.collection("rankingsSemanales")
                        .document(semanaId)
                        .collection("usuarios")
                        .document(uidUsuarioActual)
                        .get()
                        .await()

                    if (miDoc.exists()) {
                        val miXp = miDoc.getLong("xpSemanal")?.toInt() ?: 0
                        val miNombre = miDoc.getString("nombre") ?: "Tú"

                        val mayorCount = firestore.collection("rankingsSemanales")
                            .document(semanaId)
                            .collection("usuarios")
                            .whereGreaterThan("xpSemanal", miXp)
                            .get()
                            .await()
                            .size()

                        _miRankingFueraDeTop.value = UsuarioRanking(
                            uid = uidUsuarioActual,
                            nombre = miNombre,
                            xpSemanal = miXp,
                            posicion = mayorCount + 1,
                        )
                    }
                }
            } catch (e: Exception) {
                _ranking.value = Recurso.Error(
                    "No se pudo cargar el ranking. Inténtalo nuevamente."
                )
            }
        }
    }

    fun reintentar(uidUsuarioActual: String? = null) = cargarRanking(uidUsuarioActual)

    companion object {
        private val ZONA_LIMA = ZoneId.of("America/Lima")

        /** Devuelve el ID de la semana actual en formato ISO, ej: "2026-W45". */
        fun obtenerSemanaActual(): String {
            val hoy = LocalDate.now(ZONA_LIMA)
            val campos = WeekFields.ISO
            val anio = hoy.get(campos.weekBasedYear())
            val semana = hoy.get(campos.weekOfWeekBasedYear())
            return String.format(Locale.US, "%d-W%02d", anio, semana)
        }
    }
}
