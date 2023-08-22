data class Piloto(
    val codigo: String,
    val nome: String
)

data class Volta(
    val numero: Int,
    val tempo: String,
    val velocidadeMedia: Double
)

data class RegistroVolta(
    val hora: String,
    val piloto: Piloto,
    val volta: Volta
)

data class RegistroResultado(
    val piloto: Piloto,
    val totalVoltas: Int,
    val tempoTotal: Double
)

fun main() {
    val log = """
23:49:08.277 038 – F.MASSA 1 1:02.852 44,275
23:49:10.858 033 – R.BARRICHELLO 1 1:04.352 43,243
23:49:11.075 002 – K.RAIKKONEN 1 1:04.108 43,408
23:49:12.667 023 – M.WEBBER 1 1:04.414 43,202
23:49:30.976 015 – F.ALONSO 1 1:18.456 35,47
23:50:11.447 038 – F.MASSA 2 1:03.170 44,053
23:50:14.860 033 – R.BARRICHELLO 2 1:04.002 43,48
23:50:15.057 002 – K.RAIKKONEN 2 1:03.982 43,493
23:50:17.472 023 – M.WEBBER 2 1:04.805 42,941
23:50:37.987 015 – F.ALONSO 2 1:07.011 41,528
23:51:14.216 038 – F.MASSA 3 1:02.769 44,334
23:51:18.576 033 – R.BARRICHELLO 3 1:03.716 43,675
23:51:19.044 002 – K.RAIKKONEN 3 1:03.987 43,49
23:51:21.759 023 – M.WEBBER 3 1:04.287 43,287
23:51:46.691 015 – F.ALONSO 3 1:08.704 40,504
23:52:01.796 011 – S.VETTEL 1 3:31.315 13,169
23:52:17.003 038 – F.MASS 4 1:02.787 44,321
23:52:22.586 033 – R.BARRICHELLO 4 1:04.010 43,474
23:52:22.120 002 – K.RAIKKONEN 4 1:03.076 44,118
23:52:25.975 023 – M.WEBBER 4 1:04.216 43,335
23:53:06.741 015 – F.ALONSO 4 1:20.050 34,763
23:53:39.660 011 – S.VETTEL 2 1:37.864 28,435
23:54:57.757 011 – S.VETTEL 3 1:18.097 35,633
    """.trimIndent()

    val registros = log.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { criarRegistroVolta(it) }

    val (resultado, melhorVoltaGeral) = gerarResultado(registros)
    println("")
    resultado.forEach { println(it) }
    println("")
    melhorVoltaGeral?.let {
        println("Melhor volta geral de todas: Tempo ${it.tempo} - Velocidade média ${it.velocidadeMedia}")
    }
}

fun criarRegistroVolta(registro: String): RegistroVolta {
    val partes = registro.split(" ")
    val hora = partes[0]
    val codigoPiloto = partes[1]
    val nomePiloto = partes[3]
    val numeroVolta = partes[4].toInt()
    val tempoVolta = partes[5]
    val velocidadeMedia = partes[6].replace(",", ".").toDouble()

    return RegistroVolta(
        hora,
        Piloto(codigoPiloto, nomePiloto),
        Volta(numeroVolta, tempoVolta, velocidadeMedia)
    )
}

fun gerarResultado(registros: List<RegistroVolta>): Pair<List<String>, Volta?> {
    val voltasPorPiloto = mutableMapOf<String, MutableList<Volta>>()

    registros.forEach { registro ->
        voltasPorPiloto
            .getOrPut(registro.piloto.codigo) { mutableListOf() }
            .add(registro.volta)
    }

    val resultado = mutableListOf<RegistroResultado>()

    voltasPorPiloto.forEach { (codigoPiloto, voltas) ->
        val piloto = registros.find { it.piloto.codigo == codigoPiloto }!!.piloto
        val totalVoltas = voltas.size
        val tempoTotal = voltas.sumByDouble { tempoParaSegundos(it.tempo) }
        resultado.add(RegistroResultado(piloto, totalVoltas, tempoTotal))
    }

    val resultadoOrdenado = resultado.sortedWith(compareByDescending<RegistroResultado> { it.totalVoltas }.thenBy { it.tempoTotal })
    val resultadoFinal = mutableListOf<String>()

    val melhoresVoltas = obterMelhoresVoltas(registros)
    var melhorVoltaGeral: Volta? = null

    resultadoOrdenado.forEachIndexed { index, registroResultado ->
        val posicao = index + 1
        val tempoTotalFormatado = segundosParaTempo(registroResultado.tempoTotal)

        val melhorVolta = melhoresVoltas[registroResultado.piloto]

        if (melhorVolta != null && (melhorVoltaGeral == null || tempoParaSegundos(melhorVolta.tempo) < tempoParaSegundos(melhorVolta.tempo))) {
            melhorVoltaGeral = melhorVolta
        }

        val melhorVoltaFormatada = if (melhorVolta != null) "Melhor volta: ${melhorVolta.tempo}" else ""

        resultadoFinal.add("$posicao º ${registroResultado.piloto.codigo} - ${registroResultado.piloto.nome} - ${registroResultado.totalVoltas} voltas - Tempo total: $tempoTotalFormatado $melhorVoltaFormatada")
    }

    return Pair(resultadoFinal, melhorVoltaGeral)
}

fun tempoParaSegundos(tempo: String): Double {
    val partes = tempo.split(":")
    val minutos = partes[0].toInt()
    val segundosParte = partes[1].split(".")
    val segundos = segundosParte[0].toInt()
    val milissegundos = segundosParte[1].toDouble() / 1000.0
    return minutos * 60 + segundos + milissegundos
}

fun segundosParaTempo(segundos: Double): String {
    val minutos = segundos.toInt() / 60
    val segundosRestantes = segundos.toInt() % 60
    val milissegundos = ((segundos - segundos.toInt()) * 1000).toInt()
    return String.format("%d:%02d.%03d", minutos, segundosRestantes, milissegundos)
}

fun obterMelhoresVoltas(registros: List<RegistroVolta>): Map<Piloto, Volta> {
    val melhoresVoltasPorPiloto = mutableMapOf<Piloto, Volta>()

    registros.forEach { registro ->
        val piloto = registro.piloto
        val volta = registro.volta

        val melhorVoltaAtual = melhoresVoltasPorPiloto[piloto]

        if (melhorVoltaAtual == null || tempoParaSegundos(volta.tempo) < tempoParaSegundos(melhorVoltaAtual.tempo)) {
            melhoresVoltasPorPiloto[piloto] = volta
        }
    }

    return melhoresVoltasPorPiloto
}