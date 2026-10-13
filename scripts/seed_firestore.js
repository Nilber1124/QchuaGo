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

const datos = [
  {
    id: "unidad_1",
    campos: { nombre: "Unidad 1: Saludos", descripcion: "Napaykunakuy · Aprende a saludar en quechua", icono: "💬", colorHex: "#388E3C", orden: 1 },
    lecciones: [
      { id: "lec_1",  nombre: "Rimaykullayki", icono: "👋", orden: 1, tipo: "leccion" },
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

async function sembrar() {
  console.log("🌱 Actualizando datos en Firestore...\n");
  for (const u of datos) {
    await db.collection("unidades").doc(u.id).set(u.campos);
    console.log(`✅ ${u.campos.nombre}  ${u.campos.icono}`);
    for (const l of u.lecciones) {
      const { id, ...campos } = l;
      await db.collection("unidades").doc(u.id).collection("lecciones").doc(id).set(campos);
      console.log(`   ➕ ${campos.nombre}  ${campos.icono}`);
    }
  }
  console.log("\n🎉 ¡Listo!");
  process.exit(0);
}

sembrar().catch((e) => { console.error("❌", e.message); process.exit(1); });
