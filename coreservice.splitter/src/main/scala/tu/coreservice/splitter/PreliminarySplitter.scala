package tu.coreservice.splitter

import _root_.relex.corpus.{DocSplitterFactory, DocSplitter}
import tu.coreservice.action.way2think.Way2Think
import tu.model.knowledge.primitive.KnowledgeString
import tu.model.knowledge.communication.{ContextHelper, Context}
import tu.model.knowledge.{Resource, KnowledgeURI}
import tu.coreservice.spellcorrector.SpellCorrector
import relex.entity.EntityMaintainer
import relex.RelationExtractor
import relex.output.OpenCogScheme
import scala.collection.JavaConversions._
import tu.coreservice.action.way2think.cry4help.Cry4HelpWay2Think
import org.slf4j.LoggerFactory
import relex.feature.FeatureNode
import tu.model.knowledge.annotator.{AnnotatedNarrative, AnnotatedSentence, AnnotatedPhrase}


/**
 * @author toschev alex
 *         Date: 01.06.12
 *         Time: 18:59
 *
 */

/**
 * split text in sentence
 * https://github.com/development-team/2/blob/master/doc/design-specification/splitting-text-to-sentences.md
 */
class PreliminarySplitter extends Way2Think {

  val log = LoggerFactory.getLogger(this.getClass)

  def setup: RelationExtractor = {
    // relex.RelationExtractor -n 4 -l -t -f -r -a
    val re = new RelationExtractor(false)
    // -n 4
    re.setMaxParses(1)
    // -l -f -a
    val opencog: OpenCogScheme = new OpenCogScheme()
    opencog.setShowLinkage(true)
    opencog.setShowFrames(true)
    re.do_anaphora_resolution = true
    opencog.setShowAnaphora(true)
    // -t
    re.do_tree_markup = true
    re.do_pre_entity_tagging = true
    re.do_post_entity_tagging = true
    re
  }


  /**
   * Apply Way2Think.
   * @param inputContext Context inbound.
   * @param outputContext Context outbound.
   * @return split in sentence text
   * @deprecated
   */
  def apply(inputContext: Context, outputContext: Context) {
    //extract text from input context
    val textFrame = inputContext.frames.filter(p =>
      p._1.name == "inputtext"
    ).head

    //initialize output context
    ContextHelper.initializeContext(outputContext)

    val sentenceURI = new KnowledgeURI("tu-project.com", "sentence", "0.3")


    // split text using relex
    val ds: DocSplitter = DocSplitterFactory.create()

    //correct all text before splitting to sentence
    var text = textFrame._2.asInstanceOf[KnowledgeString].value

    val corrector = SpellCorrector()
    text = corrector.correctSentence(text)

    ds.addText(text)

    var sntOrder = 1


    var sentence = ds.getNextSentence
    while (sentence != null) {
      //TODO: USING RelEx split by sentences
      //and place this to context

      //check sentence using autocorrector
      //append extracted sentence to context and increase counter for sentence
      outputContext.frames += (new KnowledgeURI("tu-project.com", sentenceURI.name + "-" + sntOrder, "0.3") -> new KnowledgeString(sentence, sentenceURI))
      sntOrder = sntOrder + 1
      sentence = ds.getNextSentence
    }

  }


  def processSentences(sentence: String) {

    var em: EntityMaintainer = new EntityMaintainer()

    var relExt = setup

  }

  /**
   * Way2Think interface.
   * @param inputContext the Context to process.
   * @return outputContext
   */
  def apply(inputContext: Context): Context = {

    log info "apply(" + inputContext + ": Context)"

    val textFrames = inputContext.frames.filter(p =>
      p._1.name == "inputtext"
    )

    val textFrame = if (textFrames.size > 0) {
      textFrames.head
    } else {
      val cry4Help = Cry4HelpWay2Think("$Could_not_find " + "inputtext")
      val outputContext = ContextHelper(List(cry4Help), this.getClass.getName + " result")
      return outputContext
    }

    val sentenceURI = new KnowledgeURI("tu-project.com", "sentence", "0.3")

    // split text using relex
    val ds: DocSplitter = DocSplitterFactory.create()

    //correct all text before splitting to sentence
    var text = textFrame._2.asInstanceOf[KnowledgeString].value

    val corrector = SpellCorrector()
    text = corrector.correctSentence(text)

    ds.addText(text)

    var sntOrder = 1

    var sentence: String = ds.getNextSentence
    val outputContext = ContextHelper(List[Resource](), this.getClass.getName)
    var annotatedSentences: List[AnnotatedSentence] = List[AnnotatedSentence]()
    while (sentence != null) {
      //check sentence using auto-corrector
      //append extracted sentence to context and increase counter for sentence

      //run relex and extract sentences
      val em: EntityMaintainer = new EntityMaintainer()
      val relExt = setup
      val relexSentence = relExt.processSentence(sentence, em)
      val tree = relexSentence.getParses.get(0).getPhraseTree
      //extract all sentences
      val phrases: List[AnnotatedPhrase] = tree.iterator().map(
        u => {
          val wordList = u.getWordList
          val phrs: List[AnnotatedPhrase] = wordList.map {
            w: FeatureNode => {
              //append word
              val phrase: AnnotatedPhrase = AnnotatedPhrase(w.get("orig_str").getValue)
              phrase
            }
          }.toList
          phrs
        }
      ).toList.flatten

      //relexSentence.getParses.toList.map(b=>new Phrase(b.getPhraseTree.))
      //var convertedPhrases = relexSentence.getParses.toArray.map(b=> new Phrase(b.))
      outputContext.frames += (new KnowledgeURI("tu-project.com", sentenceURI.name + "-" + sntOrder, "0.3")
        -> new KnowledgeString(sentence, sentenceURI))

      //also add sentences to sentence
      val annotatedSentence = new AnnotatedSentence(phrases, new KnowledgeURI("tu-project.com", sentenceURI.name + "-" + sntOrder, "0.3"))
      sntOrder = sntOrder + 1
      sentence = ds.getNextSentence
      annotatedSentences ::= annotatedSentence
    }
    val annotatedNarrative = AnnotatedNarrative(annotatedSentences, KnowledgeURI(this.getClass.getName + " result"))
    outputContext.lastResult = Some(annotatedNarrative)
    log info "apply():" + outputContext
    outputContext
  }

  def start() = false

  def stop() = false
}
