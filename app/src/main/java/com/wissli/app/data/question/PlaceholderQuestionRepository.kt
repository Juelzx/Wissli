package com.wissli.app.data.question

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// TODO: replace with a Firestore implementation (questions + categories).
// Every level of every category reuses the same sample questions, only their ids differ.
class PlaceholderQuestionRepository : QuestionRepository {
    override fun observeDailyQuestion(): Flow<DailyQuestion?> =
        flowOf(
            DailyQuestion(
                questionId = "sky-blue",
                title = "Warum ist der Himmel blau?",
                categoryName = "Natur",
                ageFrom = 6,
                ageTo = 10,
            ),
        )

    override fun observeCategories(): Flow<List<Category>> =
        flowOf(
            listOf(
                Category(id = "nature", name = "Natur", order = 0, levelCount = 12),
                Category(id = "space", name = "Weltall", order = 1, levelCount = 10),
                Category(id = "body", name = "Körper", order = 2, levelCount = 8),
                Category(id = "animals", name = "Tiere", order = 3, levelCount = 12),
            ),
        )

    override suspend fun getLevelQuestions(
        categoryId: String,
        level: Int,
    ): List<Question> =
        SampleQuestions.mapIndexed { index, sample ->
            Question(
                id = "$categoryId-$level-${index + 1}",
                categoryId = categoryId,
                level = level,
                order = index + 1,
                title = sample.title,
                answers = sample.answers,
                correctAnswerIndex = sample.correctAnswerIndex,
                explanation = sample.explanation,
            )
        }
}

private class SampleQuestion(
    val title: String,
    val answers: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
)

private val SampleQuestions =
    listOf(
        SampleQuestion(
            "Warum ist der Himmel blau?",
            listOf(
                "Er spiegelt das Meer",
                "Blaues Licht wird stärker gestreut",
                "Die Sonne ist blau",
                "Wegen der Wolken",
            ),
            1,
            "Sonnenlicht enthält alle Farben. Die Luft streut kurzwelliges blaues Licht viel stärker als rotes – " +
                "deshalb kommt es von überall am Himmel zu uns.",
        ),
        SampleQuestion(
            "Warum fallen im Herbst die Blätter?",
            listOf("Der Baum spart Wasser", "Der Wind reißt sie ab", "Sie werden zu schwer", "Vögel fressen sie"),
            0,
            "Im Winter kommt der Baum schlecht an Wasser. Über Blätter würde er viel davon verdunsten, " +
                "also wirft er sie ab und macht Pause.",
        ),
        SampleQuestion(
            "Warum ist Meerwasser salzig?",
            listOf(
                "Fische geben Salz ab",
                "Flüsse spülen Salz aus Gestein hinein",
                "Salz fällt mit dem Regen",
                "Es ist gar nicht salzig",
            ),
            1,
            "Regen löst winzige Mengen Salz aus Steinen. Flüsse tragen es ins Meer, dort verdunstet das Wasser – " +
                "das Salz bleibt zurück.",
        ),
        SampleQuestion(
            "Warum blitzt es bei einem Gewitter?",
            listOf(
                "Wolken reiben an Bergen",
                "Elektrische Ladung entlädt sich",
                "Die Sonne spiegelt sich",
                "Regentropfen leuchten",
            ),
            1,
            "In Gewitterwolken reiben Eiskörnchen aneinander und laden sich auf. Wird die Spannung zu groß, " +
                "springt ein riesiger Funke über: der Blitz.",
        ),
        SampleQuestion(
            "Warum hören wir den Donner später als wir den Blitz sehen?",
            listOf(
                "Schall ist langsamer als Licht",
                "Der Donner kommt zuerst",
                "Die Ohren sind langsamer",
                "Wolken bremsen das Licht",
            ),
            0,
            "Licht ist fast eine Million Mal schneller als Schall. Zählst du die Sekunden bis zum Donner und " +
                "teilst durch drei, weißt du ungefähr, wie viele Kilometer das Gewitter entfernt ist.",
        ),
        SampleQuestion(
            "Warum gibt es einen Regenbogen?",
            listOf(
                "Wolken sind bunt",
                "Regentropfen zerlegen Sonnenlicht",
                "Der Himmel spiegelt Blumen",
                "Die Sonne malt ihn",
            ),
            1,
            "Jeder Regentropfen wirkt wie ein kleines Prisma: Er bricht das Sonnenlicht und fächert es in seine " +
                "Farben auf.",
        ),
        SampleQuestion(
            "Warum schwimmt Eis auf dem Wasser?",
            listOf(
                "Eis ist leichter als gleich viel Wasser",
                "Eis enthält Luftballons",
                "Wasser drückt es hoch, weil es kalt ist",
                "Eis ist magnetisch",
            ),
            0,
            "Beim Gefrieren ordnen sich Wassermoleküle mit mehr Abstand an. Eis hat deshalb eine geringere " +
                "Dichte als flüssiges Wasser und schwimmt oben.",
        ),
        SampleQuestion(
            "Warum ist es im Sommer wärmer als im Winter?",
            listOf(
                "Die Erde ist der Sonne näher",
                "Die Sonne steht höher am Himmel",
                "Die Sonne brennt stärker",
                "Es gibt weniger Wolken",
            ),
            1,
            "Die Erdachse ist schief. Im Sommer ist unsere Hälfte zur Sonne geneigt, die Strahlen treffen steiler " +
                "auf und die Tage sind länger.",
        ),
        SampleQuestion(
            "Warum sind Pflanzen grün?",
            listOf(
                "Sie trinken grünes Wasser",
                "Chlorophyll wirft grünes Licht zurück",
                "Die Erde färbt sie",
                "Sie sind gar nicht grün",
            ),
            1,
            "Der Blattfarbstoff Chlorophyll nutzt rotes und blaues Licht zur Energiegewinnung. Grünes Licht " +
                "braucht er kaum – es wird zurückgeworfen und landet in unseren Augen.",
        ),
        SampleQuestion(
            "Warum gibt es Ebbe und Flut?",
            listOf("Der Mond zieht am Wasser", "Wale schieben das Wasser", "Der Wind dreht sich", "Flüsse laufen über"),
            0,
            "Die Anziehungskraft des Mondes zieht das Meerwasser zu einem Berg. Weil sich die Erde dreht, " +
                "wandern diese Berge über die Küsten – zweimal am Tag kommt Flut.",
        ),
        SampleQuestion(
            "Warum leuchten Glühwürmchen?",
            listOf(
                "Sie speichern Sonnenlicht",
                "Ein chemischer Stoff leuchtet in ihrem Körper",
                "Sie haben kleine Batterien",
                "Mondlicht spiegelt sich",
            ),
            1,
            "In ihrem Hinterleib reagiert der Stoff Luciferin mit Sauerstoff. Dabei entsteht kaltes Licht, " +
                "mit dem sie Partner anlocken.",
        ),
        SampleQuestion(
            "Warum entsteht Nebel?",
            listOf(
                "Wolken fallen herunter",
                "Feuchte Luft kühlt ab und bildet Tröpfchen",
                "Rauch steigt aus dem Boden",
                "Der Wind wirbelt Staub auf",
            ),
            1,
            "Kühlt feuchte Luft ab, kann sie nicht mehr so viel Wasserdampf halten. Winzige Tröpfchen schweben " +
                "dann in Bodennähe – im Grunde ist Nebel eine Wolke am Boden.",
        ),
        SampleQuestion(
            "Warum ist Schnee weiß?",
            listOf(
                "Er besteht aus Milch",
                "Eiskristalle streuen alle Farben gleich",
                "Die Wolken sind weiß",
                "Er ist eigentlich blau",
            ),
            1,
            "Schnee besteht aus unzähligen Eiskristallen. Ihre vielen Flächen werfen alle Farben des Lichts " +
                "gleichmäßig zurück – und alle Farben zusammen sehen wir als Weiß.",
        ),
        SampleQuestion(
            "Warum haben Bäume Jahresringe?",
            listOf(
                "Im Frühling und Herbst wächst das Holz verschieden",
                "Sie werden jedes Jahr bemalt",
                "Würmer bohren Kreise",
                "Die Rinde rutscht nach innen",
            ),
            0,
            "Im Frühling wächst helles, lockeres Holz, im Spätsommer dunkleres, festes. Zusammen ergibt das " +
                "einen Ring pro Jahr.",
        ),
        SampleQuestion(
            "Warum rostet Eisen?",
            listOf(
                "Es reagiert mit Sauerstoff und Wasser",
                "Es wird zu alt",
                "Bakterien malen es braun",
                "Sonnenlicht färbt es",
            ),
            0,
            "Eisen verbindet sich mit Sauerstoff aus Luft und Wasser zu Eisenoxid. Diese bröselige, rotbraune " +
                "Schicht nennen wir Rost.",
        ),
        SampleQuestion(
            "Warum duften Blumen?",
            listOf(
                "Um Insekten anzulocken",
                "Um Regen anzuziehen",
                "Weil sie gewaschen wurden",
                "Um andere Blumen zu warnen",
            ),
            0,
            "Mit ihrem Duft locken Blüten Bienen und andere Insekten an. Die tragen beim Naschen Pollen weiter " +
                "und helfen so bei der Bestäubung.",
        ),
        SampleQuestion(
            "Warum wehen Winde?",
            listOf("Bäume fächeln Luft", "Luft strömt von hohem zu tiefem Druck", "Das Meer pustet", "Die Erde atmet"),
            1,
            "Die Sonne erwärmt die Erde ungleichmäßig. Warme Luft steigt auf, kühlere strömt nach – " +
                "diese Ausgleichsbewegung spüren wir als Wind.",
        ),
        SampleQuestion(
            "Warum sind Vulkane heiß?",
            listOf(
                "Sie stehen in der Sonne",
                "Geschmolzenes Gestein kommt aus der Tiefe",
                "Blitze schlagen ein",
                "Es brennt Holz in ihnen",
            ),
            1,
            "Tief unter der Erdkruste ist es so heiß, dass Gestein schmilzt. Bei einem Ausbruch drückt dieses " +
                "Magma nach oben und tritt als Lava aus.",
        ),
        SampleQuestion(
            "Warum hat der Mond Krater?",
            listOf(
                "Dort wurde gebaut",
                "Gesteinsbrocken aus dem All schlugen ein",
                "Der Mond ist aus Käse",
                "Wasser hat Löcher gespült",
            ),
            1,
            "Der Mond hat keine schützende Lufthülle. Meteoriten verglühen nicht, sondern schlagen ein – und " +
                "ohne Wind und Regen bleiben die Krater Milliarden Jahre erhalten.",
        ),
        SampleQuestion(
            "Warum zirpen Grillen?",
            listOf(
                "Sie reiben ihre Flügel aneinander",
                "Sie pfeifen mit dem Mund",
                "Sie klopfen auf Steine",
                "Sie haben Glöckchen",
            ),
            0,
            "Männliche Grillen streichen eine geriffelte Kante des einen Flügels über den anderen – wie ein " +
                "Bogen über eine Geige. So rufen sie nach Weibchen.",
        ),
    )
