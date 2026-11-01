package org.ulpgc.bigdata.indexing;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class IndexingBenchmark {

    private IndexManager indexManager;
    private String sampleText;
    private int currentBookId;

    @Setup(Level.Iteration)
    public void setUp() {
        indexManager = new IndexManager();
        // Generamos la misma carga masiva (55.000 palabras)
        sampleText = "Este es un texto de prueba para el sistema de Big Data. ".repeat(5000);
        currentBookId = 0; // Reiniciamos el contador en cada bloque de pruebas
    }

    @Benchmark
    public void testIndexingSpeed(Blackhole bh) {
        currentBookId++; // Simulamos un libro distinto en cada ejecución

        indexManager.indexText(currentBookId, sampleText);

        // Obligamos a la JVM a leer el tamaño del índice para que no descarte la operación
        bh.consume(indexManager.getIndex().getIndex().size());
    }
}