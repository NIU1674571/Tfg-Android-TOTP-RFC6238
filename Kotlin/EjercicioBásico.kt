/* 
==============================
Ejercicio básico de Kotlin
Conceptos practicados:
- Variables (var) vs constantes (val)
- Tipos: String, Int, Double, Float, Boolean (inferidos y explícitos)
- Interpolación de strings: "texto $variable"
- Control de flujo: if / else if / else + operadores && y ||
- Colecciones: List (mutable), Set (sin duplicados), Map (clave-valor)
- Bucles: for y while con contador
- Null safety: tipos anulables (String?) y uso de null
- Funciones: declarar y llamar funciones
- Clases: constructor primario y propiedades (val)
==============================
*/

fun main() {

    println("Hola Kotlin!")

    // ------------------------------
    // Variables y tipos
    // ------------------------------

    // var = variable mutable se puede reasignar pero con el mismo tipo de dato
    var myString = "Esto es una cadena de texto"
    myString = "Aquí cambio el valor de la cadena de texto"
    // myString = 6 // Error: Kotlin detecta que myString es String por eso no deja reasignar un int
    println(myString)

    // Tipo explícito: se fuerza a que sea String, normalmente no se utiliza pero en casos puntuales es útil
    var myString2: String = "Esta es otra cadena de texto"
    println(myString2)

    // Tipo inferido: Kotlin deduce que es Int
    var myInt = 7
    myInt = myInt + 4
    println(myInt)
    println(myInt - 1)
    println(myInt)

    // Interpolación de strings con $variable
    println("Este es el valor de la variable myInt: $myInt")

    // Tipo explícito Int
    var myInt2: Int = 5
    println(myInt2)

    // Double (decimal) inferido: si asignamos decimales a un var pues será tipo de dato Double
    var myDouble = 6.5
    println(myDouble)

    // Kotlin permite reasignar con otro Double (6.0 sigue siendo Double), si le asinas 6 no te lo permite
    myDouble = 6.0
    println(myDouble)

    // Tipo explícito Double y Float (Float lleva sufijo f)
    var myDouble2: Double = 6.5
    var myFloat = 6.5f

    // Boolean
    var myBool = false
    myBool = true
    println(myBool)

    // ------------------------------
    // Constantes
    // ------------------------------

    // val = constante inmutable (no se puede reasignar)
    val myConst = "Mi propiedad constante"
    // myConst = "Mi nueva propiedad constante" // Error ya que no se puede reasignar a un constante

    // ------------------------------
    // Control de flujo
    // ------------------------------

    // if / else if / else + && (AND) y || (OR): como en C++ o lua
    if (myInt == 10 && myString == "Hola") 
    {
        println("El valor es 10")
    } else if (myInt == 11 || myString == "Hola") 
    {
        println("El valor es 11")
    } else 
    {
        println("El valor no es ni 10 ni 11")
    }

    // ------------------------------
    // Colecciones
    // ------------------------------

    // Lista mutable: permite añadir/eliminar elementos y acceso por índice
    val myList = mutableListOf("Brais", "Moure", "@mouredev")
    println(myList[1]) // acceso por índice
    myList.add("Brais") // añadir elemento
    println(myList)

    // Set: colección sin duplicados (si repites "Brais", solo aparece una vez)
    val mySet = setOf("Brais", "Moure", "@mouredev", "Brais")
    println(mySet) // Brais, Moure, @mouredev

    // Map: clave-valor (diccionario). mutableMapOf permite añadir/modificar
    val myMap = mutableMapOf("Brais" to 36, "Srodriguez" to 27, "manujb_29" to 34)
    myMap["Roswell468"] = 17 // añadir o actualizar por clave
    println(myMap["manujb_29"]) // acceso por clave

    // ------------------------------
    // Bucles
    // ------------------------------

    // for sobre lista, muy parecido a python
    for (value in myList) {
        println(value)
    }

    // for sobre set
    for (value in mySet) {
        println(value)
    }

    // for sobre map (iteras pares clave-valor)
    for (value in myMap) {
        println(value)
    }

    // while con contador (útil cuando necesitas índice)
    var myCounter = 0
    while (myCounter < myList.count()) {
        println(myList[myCounter])
        myCounter++
    }

    // ------------------------------
    // Null safety (opcionales)
    // ------------------------------

    // String? = tipo anulable (puede ser null). Base para evitar NPEs.
    var myOptional: String? = null
    println(myOptional)
    myOptional = "Mi cadena de texto opcional"
    println(myOptional)

    // ------------------------------
    // Funciones
    // ------------------------------

    // Llamo a una función definida fuera del main
    myFunction()

    // ------------------------------
    // Clases
    // ------------------------------

    // Crear instancia de una clase y acceder a la propiedad de age
    val myClass = MyClass("Brais", 36)
    println(myClass.age)
}

// Declaración de función
fun myFunction() {
    println("Esto es una función")
}

// Clase con constructor primario y propiedades (val = inmutables)
class MyClass(val name: String, val age: Int)
