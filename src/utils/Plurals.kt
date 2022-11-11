package utils

/*
 * "Если вы столяр, который создает прекрасный комод,
 *  вы не сделаете заднюю стенку из листа фанеры,
 *  хотя она и обращена к стене и ее никто никогда не увидит.
 *  Вы знаете, что она просто есть, поэтому сделаете ее из прекрасного дерева
 *  и напишите движок числительных с падежами и родами" – Стив Джобс
 */
fun Int.pluralForum(
    form1: String,
    form2: String,
    form3: String =form2
    ,
): String {
    val preLastDigit = this % 100 / 10
    if (preLastDigit == 1) {
        return String.format("%d %s", this, form3)
    }
    return when (this % 10) {
        1 -> String.format("%d %s", this, form1)
        2, 3, 4 -> String.format("%d %s", this, form2)
        else -> String.format("%d %s", this, form3)
    }
}