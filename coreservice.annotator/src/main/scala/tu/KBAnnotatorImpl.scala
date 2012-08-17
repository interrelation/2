package tu.coreservice.annotator

import org.slf4j.LoggerFactory
import tu.coreservice.action.way2think.Way2Think
import tu.model.knowledge.communication.{ContextHelper, Context}
import tu.providers.AnnotatorRegistry
import tu.model.knowledge.annotator.{AnnotatedNarrative, AnnotatedPhrase}
import tu.coreservice.utilities.URIHelper

/**
 * Simple KBAnnotator implementation.
 * @author toschev alex
 *         Date: 15.06.12
 *         Time: 18:26
 *
 */
class KBAnnotatorImpl extends Way2Think {

  val log = LoggerFactory.getLogger(this.getClass)

  /**
   * Way2Think interface.
   * @param inputContext Context of all inbound parameters.
   * @return outputContext
   */
  def apply(inputContext: Context): Context = {
    //we have output context from splitter
    //splitter return annotated sentences array

    val sentences = inputContext.frames.filter(p => p._1.name.contains(URIHelper.splitterMark()))
    val extractedNarratives = sentences.map(b => b._2.asInstanceOf[AnnotatedNarrative])
    var wordsDetected = 0
    //trying to annotate sentences
    val localAnnotator = AnnotatorRegistry.getLocalAnnotator()

    def checkLocalKB(phrase: String): Option[AnnotatedPhrase] = {
      val localAnnotated = localAnnotator.apply(phrase)
      localAnnotated
    }


    def annotatePhrase(ph:AnnotatedPhrase):Boolean={
      var result=false
      var annotationFound = checkLocalKB(ph.phrase)
      def appendAnnotation(ref: AnnotatedPhrase, src: AnnotatedPhrase) {
        ref.concepts = src.concepts
        //ref.phrases = src.phrases

      }
      if (annotationFound.isEmpty) {
        AnnotatorRegistry.listAnnotators().filter(p => p.isLocal() != true).foreach(a => {
          val synonymous = a.annotate(ph.phrase)
          synonymous.foreach(syn => {
            //check against local KB
            annotationFound = checkLocalKB(syn)
            if (!annotationFound.isEmpty) {
              appendAnnotation(ph, annotationFound.get)
              result=true
              scala.util.control.Breaks.break()
            }
          })
        })
      }
      else {
        appendAnnotation(ph, annotationFound.get)
        result=true
      }
      return result
    }


    def recurrentlyCheckPhrase(ph:AnnotatedPhrase):Boolean={
      var annotateFound = annotatePhrase(ph)
      if (!annotateFound)
      {

          ph.phrases.foreach(ph1=>{
            recurrentlyCheckPhrase(ph1)
          })

      }
      true
    }

    extractedNarratives.foreach(n => {
      val extractedSentences = n.sentences
      extractedSentences.foreach(s => {
        val extractedPhrases = s.phrases
        extractedPhrases.foreach(curPhrase => {
          recurrentlyCheckPhrase(curPhrase)

        })
      })
    })
    val ctxOutput = ContextHelper(sentences.map(b => b._2).toList, sentences.head._2, "AnnotationResult")
    ctxOutput
  }

  def start() = false

  def stop() = false
}
