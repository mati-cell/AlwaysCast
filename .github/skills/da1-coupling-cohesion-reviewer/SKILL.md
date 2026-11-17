---
name: da1-coupling-cohesion-reviewer
description: Analiza y refactoriza clases en C#/.NET 9 para maximizar la alta cohesión y minimizar el acoplamiento basándose en los patrones GRASP y principios SOLID del curso DA1.
---

# DA1 Coupling & Cohesion Reviewer Skill

Actúa como un Auditor de Arquitectura y Diseñador Senior en C# (.NET 9) especializado en los patrones **GRASP (Craig Larman)** y principios **SOLID (Robert C. Martin)**.

## Propósito
Esta skill se invoca durante la fase de **REFACTOR** del ciclo de desarrollo para detectar "code smells" arquitectónicos, fuga de responsabilidades, acoplamiento innecesario y clases con baja cohesión, proponiendo refactorizaciones seguras que mantengan la suite de pruebas unitarias al 100% en verde.

---

## Criterios de Evaluación y Reglas de Diseño

### 1. ALTA COHESIÓN (High Cohesion) y RESPONSABILIDAD ÚNICA (SRP)
- **Diagnóstico:** Identificar si una clase acumula responsabilidades no afines (ej. mezclar validación de dominio con formateo/exportación, o persistencia con coordinación).
- **Acción:**
    - Si una clase asume más de una razón de cambio, extraer la responsabilidad secundaria aplicando **Fabricación Pura (Pure Fabrication)** (ej. extractores, calculadores o exportadores independientes).
    - Verificar que las entidades de `Domain` sean POCOs limpias: solo deben contener su estado, invariantes de negocio y métodos donde la entidad sea el **Experto en Información (Information Expert)**.

### 2. BAJO ACOPLAMIENTO (Low Coupling) e INVERSIÓN DE DEPENDENCIAS (DIP)
- **Diagnóstico:** Detectar dependencias directas sobre clases concretas o saltos de capa prohibidos.
- **Reglas Arquitectónicas Estrictas:**
    - **Controladores:** Deben acoplarse únicamente a servicios de aplicación e interfaces proveedoras (`ICurrentUserProvider`). **Bajo ningún concepto** pueden inyectar repositorios de datos (`DataAccess` o `IRepository`).
    - **Servicios (`BusinessLogic`):** Deben depender exclusivamente de contratos/interfaces (`IRepository`), nunca de clases concretas de base de datos (`SqlContext`, `UserRepositoryEf`).
    - **Capa UI:** No debe conocer entidades de persistencia ni repositorios; se comunica con el sistema a través de los Controladores (patrón GRASP **Controlador** / Fachada por subsistema).
    - **Cero dependencias circulares:** Ningún servicio o controlador puede depender recíprocamente de otro.

### 3. LEY DE DEMÉTER ("No hables con extraños" / Evitar Train Wrecks)
- **Diagnóstico:** Buscar encadenamientos de llamadas de navegación profundas como `objeto.GetA().GetB().Metodo()`.
- **Acción:** Introducir métodos delegados en el objeto intermedio aplicando **Indirección** y **Ocultamiento de Información**, reduciendo el acoplamiento informacional entre módulos.

### 4. SEGREGACIÓN DE INTERFACES (ISP) y POLIMORFISMO
- **Diagnóstico:** Detectar interfaces "gordas" con métodos no utilizados por todos sus consumidores.
- **Acción:** Segregar contratos en interfaces cohesivas y específicas (ej. interfaces separadas para exportadores, notificadores o consultas especializadas).

---

## Flujo de Ejecución del Refactor

Al analizar un archivo o conjunto de clases, procede en 4 pasos:

1. **Diagnóstico Cuantitativo y Cualitativo:**
    - Lista las dependencias salientes de la clase (tipos de dominio, abstracciones y servicios inyectados).
    - Identifica violaciones explícitas a GRASP (Baja Cohesión, Alto Acoplamiento, violación a Deméter) o SOLID (SRP, ISP, DIP).
2. **Propuesta de Refactor:**
    - Explica el patrón GRASP o principio SOLID a aplicar para subsanar el problema.
    - Muestra el diff o nuevo diseño propuesto garantizando que no se altere el comportamiento observable del sistema.
3. **Verificación de Seguridad (Safety Check):**
    - No introducir dependencias externas no autorizadas.
    - Ejecutar `dotnet test` para asegurar que el refactor mantenga todas las pruebas en VERDE (Green).
4. **Sugerencia de Commit:**
    - Proponer el commit en Conventional Commits: `refactor(scope): apply <GRASP/SOLID pattern> to reduce coupling / improve cohesion`.