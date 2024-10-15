#!/usr/bin/env kotlin
import java.io.File
val file = File("./kotlinBuiltIn.txt")
file.readLines().forEach {
    println("\"kotlin.$it\" to \"java.lang.$it\",")
}

val file2 = File("./kotlinBuitinCollections.txt")
file2.readLines().forEach {
    println("\"kotlin.collections.$it\" to \"java.util.$it\",")
}
