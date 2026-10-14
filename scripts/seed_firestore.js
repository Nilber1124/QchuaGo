import admin from "firebase-admin";
import { createRequire } from "module";
import { existsSync } from "fs";

const KEY_PATH = "./serviceAccountKey.json";

if (!existsSync(KEY_PATH)) {
  console.error(
    "❌  No se encontró serviceAccountKey.json\n" +
    "   Descárgala: Firebase Console → ⚙️ Configuración → Cuentas de servicio\n" +
    "   → Generar nueva clave privada → guardar como scripts/serviceAccountKey.json"
  );
  process.exit(1);
}

const require = createRequire(import.meta.url);
admin.initializeApp({ credential: admin.credential.cert(require(KEY_PATH)) });
const db = admin.firestore();

const niveles = [
  {
    id: "nivel_1",
    campos: {
      nombre: "Básico",
      descripcion: "Fundamentos del quechua de Cajamarca",
      orden: 1,
      icono: "🌱",
      colorHex: "#4CAF50",
      activo: true,
    },
  },
  {
    id: "nivel_2",
    campos: {
      nombre: "Intermedio",
      descripcion: "Vocabulario y expresiones de dificultad intermedia",
      orden: 2,
      icono: "📘",
      colorHex: "#2196F3",
      activo: true,
    },
  },
  {
    id: "nivel_3",
    campos: {
      nombre: "Avanzado",
      descripcion: "Comprensión y comunicación de dificultad avanzada",
      orden: 3,
      icono: "🏆",
      colorHex: "#9C27B0",
      activo: true,
    },
  },
];

const unidades = [
  {
    id: "unidad_1",
    campos: { nombre: "Unidad 1: Saludos", descripcion: "Napaykunakuy · Aprende a saludar en quechua", icono: "💬", colorHex: "#388E3C", orden: 1, nivelId: "nivel_1" },
    lecciones: [
      { id: "lec_1",  nombre: "Rimaykullayki", icono: "👋", orden: 1, tipo: "leccion" },
      { id: "lec_2",  nombre: "Buenos Días",   icono: "☀️", orden: 2, tipo: "leccion" },
      { id: "lec_cp", nombre: "¡Checkpoint!",  icono: "🎯", orden: 3, tipo: "checkpoint" },
    ],
  },
  {
    id: "unidad_2",
    campos: { nombre: "Unidad 2: Familia", descripcion: "Ayllukuna · Los miembros de la familia andina", icono: "🏠", colorHex: "#C62828", orden: 2, nivelId: "nivel_1" },
    lecciones: [
      { id: "lec_1", nombre: "Mamá y Papá", icono: "👨‍👩‍👦", orden: 1, tipo: "leccion" },
      { id: "lec_2", nombre: "Hermanos",    icono: "🤝",     orden: 2, tipo: "leccion" },
      { id: "lec_3", nombre: "Abuelos",     icono: "🌿",     orden: 3, tipo: "leccion" },
    ],
  },
  {
    id: "unidad_3",
    campos: { nombre: "Unidad 3: Números", descripcion: "Yupakuna · Cuenta en quechua", icono: "🧮", colorHex: "#1565C0", orden: 3, nivelId: "nivel_1" },
    lecciones: [
      { id: "lec_1",  nombre: "Del 1 al 5",   icono: "✋", orden: 1, tipo: "leccion" },
      { id: "lec_2",  nombre: "Del 6 al 10",  icono: "👐", orden: 2, tipo: "leccion" },
      { id: "lec_cp", nombre: "¡Checkpoint!", icono: "🎯", orden: 3, tipo: "checkpoint" },
    ],
  },
];

async function crearSiNoExiste(referencia, campos) {
  return db.runTransaction(async (transaccion) => {
    const snapshot = await transaccion.get(referencia);
    if (snapshot.exists) return false;

    transaccion.create(referencia, campos);
    return true;
  });
}

async function agregarNivelSiNoExiste(referencia, nivelId) {
  return db.runTransaction(async (transaccion) => {
    const snapshot = await transaccion.get(referencia);
    if (!snapshot.exists) {
      throw new Error(`No existe la unidad ${referencia.id}.`);
    }

    const nivelActual = snapshot.get("nivelId");
    if (nivelActual !== undefined) {
      return { actualizado: false, nivelActual };
    }

    transaccion.update(referencia, { nivelId });
    return { actualizado: true, nivelActual: nivelId };
  });
}

async function sembrar() {
  console.log("🌱 Preparando datos en Firestore sin sobrescribir documentos existentes...\n");

  for (const nivel of niveles) {
    const referenciaNivel = db.collection("niveles").doc(nivel.id);
    const creado = await crearSiNoExiste(referenciaNivel, nivel.campos);
    console.log(`${creado ? "✅ Creado" : "⏭️  Conservado"}: niveles/${nivel.id}`);
  }

  for (const u of unidades) {
    const referenciaUnidad = db.collection("unidades").doc(u.id);
    const unidadCreada = await crearSiNoExiste(referenciaUnidad, u.campos);
    console.log(`${unidadCreada ? "✅ Creada" : "⏭️  Conservada"}: unidades/${u.id}`);

    const resultadoNivel = await agregarNivelSiNoExiste(referenciaUnidad, "nivel_1");
    if (resultadoNivel.actualizado) {
      console.log(`   🔗 nivelId agregado: ${resultadoNivel.nivelActual}`);
    } else if (resultadoNivel.nivelActual === "nivel_1") {
      console.log("   ⏭️  nivelId ya estaba configurado");
    } else {
      console.warn(`   ⚠️  nivelId existente conservado: ${resultadoNivel.nivelActual}`);
    }

    for (const l of u.lecciones) {
      const { id, ...campos } = l;
      const referenciaLeccion = referenciaUnidad.collection("lecciones").doc(id);
      const leccionCreada = await crearSiNoExiste(referenciaLeccion, campos);
      console.log(`   ${leccionCreada ? "➕ Creada" : "⏭️  Conservada"}: lecciones/${id}`);
    }
  }

  console.log("\n🎉 Preparación completada sin sobrescribir datos existentes.");
  process.exit(0);
}

sembrar().catch((e) => { console.error("❌", e.message); process.exit(1); });
