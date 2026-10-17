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

const unidades = [
  {
    id: "unidad_1",
    campos: { nombre: "Unidad 1: Saludos", descripcion: "Napaykunakuy · Aprende a saludar en quechua", icono: "💬", colorHex: "#388E3C", orden: 1 },
    lecciones: [
      {
        id: "lec_1",
        nombre: "Rimaykullayki",
        icono: "👋",
        orden: 1,
        tipo: "leccion",
        actividades: [
          {
            id: "act_vocabulario_1",
            tipo: "VOCABULARIO",
            orden: 1,
            activo: true,
            quechua: "Rimaykullayki",
            traduccion: "Hola",
            explicacion: "Expresión de saludo usada como dato inicial de prueba.",
            audioLocal: "",
            audioUrl: "",
            pendienteValidacionLinguistica: true,
          },
          {
            id: "act_seleccion_1",
            tipo: "SELECCION",
            orden: 2,
            activo: true,
            enunciado: "Selecciona el saludo en quechua.",
            opciones: ["Rimaykullayki", "Tupananchiskama", "Añay"],
            respuestaCorrecta: "Rimaykullayki",
            pendienteValidacionLinguistica: true,
          },
          {
            id: "act_completar_1",
            tipo: "COMPLETAR",
            orden: 3,
            activo: true,
            enunciado: "Completa el saludo: Rimay_____",
            respuestaCorrecta: "kullayki",
            pendienteValidacionLinguistica: true,
          },
          {
            id: "act_ordenar_1",
            tipo: "ORDENAR",
            orden: 4,
            activo: true,
            enunciado: "Ordena las partes del saludo.",
            elementos: ["kullayki", "Rimay"],
            respuestasCorrectas: ["Rimay", "kullayki"],
            pendienteValidacionLinguistica: true,
          },
          {
            id: "act_pronunciacion_1",
            tipo: "PRONUNCIACION",
            orden: 5,
            activo: true,
            quechua: "Rimaykullayki",
            traduccion: "Hola",
            explicacion: "Escucha la referencia cuando haya un audio disponible y luego graba tu voz.",
            audioLocal: "",
            audioUrl: "",
            pendienteValidacionLinguistica: true,
          },
          {
            id: "act_pronunciacion_tupananchiskama",
            tipo: "PRONUNCIACION",
            orden: 6,
            activo: true,
            quechua: "Tupananchiskama",
            traduccion: "Hasta volvernos a encontrar",
            audioLocal: "tupananchiskama",
            audioUrl: "",
            pendienteValidacionLinguistica: true,
          },
          {
            id: "act_pronunciacion_anay",
            tipo: "PRONUNCIACION",
            orden: 7,
            activo: true,
            quechua: "Añay",
            traduccion: "Expresión de agradecimiento",
            audioLocal: "anay",
            audioUrl: "",
            pendienteValidacionLinguistica: true,
          },
          {
            id: "act_pronunciacion_ayllukuna",
            tipo: "PRONUNCIACION",
            orden: 8,
            activo: true,
            quechua: "Ayllukuna",
            traduccion: "Familia",
            audioLocal: "ayllukuna",
            audioUrl: "",
            pendienteValidacionLinguistica: true,
          },
        ],
      },
      { id: "lec_2",  nombre: "Buenos Días",   icono: "☀️", orden: 2, tipo: "leccion" },
      { id: "lec_cp", nombre: "¡Checkpoint!",  icono: "🎯", orden: 3, tipo: "checkpoint" },
    ],
  },
  {
    id: "unidad_2",
    campos: { nombre: "Unidad 2: Familia", descripcion: "Ayllukuna · Los miembros de la familia andina", icono: "🏠", colorHex: "#C62828", orden: 2 },
    lecciones: [
      { id: "lec_1", nombre: "Mamá y Papá", icono: "👨‍👩‍👦", orden: 1, tipo: "leccion" },
      { id: "lec_2", nombre: "Hermanos",    icono: "🤝",     orden: 2, tipo: "leccion" },
      { id: "lec_3", nombre: "Abuelos",     icono: "🌿",     orden: 3, tipo: "leccion" },
    ],
  },
  {
    id: "unidad_3",
    campos: { nombre: "Unidad 3: Números", descripcion: "Yupakuna · Cuenta en quechua", icono: "🧮", colorHex: "#1565C0", orden: 3 },
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

async function sembrar() {
  console.log("🌱 Preparando datos en Firestore sin sobrescribir documentos existentes...\n");

  for (const u of unidades) {
    const referenciaUnidad = db.collection("unidades").doc(u.id);
    const unidadCreada = await crearSiNoExiste(referenciaUnidad, u.campos);
    console.log(`${unidadCreada ? "✅ Creada" : "⏭️  Conservada"}: unidades/${u.id}`);

    for (const l of u.lecciones) {
      const { id, actividades = [], ...campos } = l;
      const referenciaLeccion = referenciaUnidad.collection("lecciones").doc(id);
      const leccionCreada = await crearSiNoExiste(referenciaLeccion, campos);
      console.log(`   ${leccionCreada ? "➕ Creada" : "⏭️  Conservada"}: lecciones/${id}`);

      for (const actividad of actividades) {
        const { id: actividadId, ...camposActividad } = actividad;
        const referenciaActividad = referenciaLeccion.collection("actividades").doc(actividadId);
        const actividadCreada = await crearSiNoExiste(referenciaActividad, camposActividad);
        console.log(
          `      ${actividadCreada ? "➕ Creada" : "⏭️  Conservada"}: actividades/${actividadId}`
        );
      }
    }
  }

  console.log("\n🎉 Preparación completada sin sobrescribir datos existentes.");
  process.exit(0);
}

sembrar().catch((e) => { console.error("❌", e.message); process.exit(1); });
