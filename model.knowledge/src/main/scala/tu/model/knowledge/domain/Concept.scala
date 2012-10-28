package tu.model.knowledge.domain

import tu.model.knowledge.semanticnetwork.SemanticNetworkNode
import tu.model.knowledge.primitive.KnowledgeString
import tu.model.knowledge.annotator.AnnotatedPhrase
import util.Random
import tu.model.knowledge._
import org.slf4j.LoggerFactory
import tu.exception.UnexpectedException
import tu.model.knowledge.KBMap._
import collection.mutable.ListBuffer

/**
 * @author max talanov
 *         date 2012-06-01
 *         time: 8:50 AM
 */

case class Concept(var _generalisations: TypedKLine[Concept],
                   var _specialisations: TypedKLine[Concept],
                   var _phrases: TypedKLine[AnnotatedPhrase],
                   __content: Resource,
                   var _conceptLinks: List[ConceptLink],
                   override val _uri: KnowledgeURI,
                   override val _probability: Probability = new Probability()

                    )
  extends SemanticNetworkNode[Resource](__content, _conceptLinks, _uri, _probability) {

  def this(_generalisations: TypedKLine[Concept],
           _specialisations: TypedKLine[Concept],
           _phrases: TypedKLine[AnnotatedPhrase],
           _content: Resource,
           _links: List[ConceptLink],
           _uri: KnowledgeURI) {
    this(_generalisations, _specialisations, _phrases, _content, _links, _uri, new Probability())
  }

  def this(map: Map[String, String]) = {
    this(
      TypedKLine[Concept]("generalisation"),
      TypedKLine[Concept]("specialisation"),
      TypedKLine[AnnotatedPhrase]("phrases"),
      map.get("content") match {
        case Some(x) => KnowledgeString(x, x)
        case None => KnowledgeString(Constant.NO_NAME, Constant.NO_NAME)
      },
      List[ConceptLink](),
      new KnowledgeURI(map),
      new Probability(map)
    )
  }


  def phrases: TypedKLine[AnnotatedPhrase] = _phrases

  def phrases_=(in: TypedKLine[AnnotatedPhrase]): Concept = {
    _phrases = in
    this
  }

  def generalisations: TypedKLine[Concept] = _generalisations


  def generalisationsList:List[Concept] = {
      _generalisations.frames.values.toList
    }


  /**
   * Assigns generalisation list to specified.
   * @param conceptKline List of concepts to assign
   * @return this
   */
  def generalisations_=(conceptKline: TypedKLine[Concept]): Concept = {
    _generalisations = conceptKline
    this
  }

  /**
   * Adds generalisation concept to TypedKline[Concept]
   * @param concept a Concept to add
   * @return this
   */
  def generalisationsAdd(concept: Concept): Concept = {
    _generalisations.frames = _generalisations.frames + (concept.uri -> concept)
    this
  }

  def specialisations = _specialisations

  def specialisations_=(in: TypedKLine[Concept]): Concept = {
    _specialisations = in
    this
  }

  override def links: List[ConceptLink] = _conceptLinks

  def links_=(in: List[ConceptLink]): Concept = {
    _conceptLinks = in
    this
  }

  def setLinks(in: List[ConceptLink]): Concept = {
    _conceptLinks = in
    this
  }

  def linksAdd(link: ConceptLink): Concept = {
    _conceptLinks = _conceptLinks ::: List(link)
    // generalisations are unidirectional
    if (link.isGeneralisationLink && link.destination != null) {
       generalisationsAdd(link.destination)
    }
    this
  }

  def linkedConcepts: Set[Concept] = {
    val res: List[Concept] = for (l <- _conceptLinks) yield {
      if (l._destination == this) {
        l.source
      } else {
        l.destination
      }
    }
    res.toSet
  }

  /**
   * Returns true if current Concept has at least one same parent(generalisation) with specified.
   * @param that Concept to compare
   * @return Boolean true if that has at least one common parent(generalisation).
   */
  def hasSameGeneralisation(that: Concept): Boolean = {
    val commonGeneralisations = that.generalisations.frames.filter {
      uriConcept: Pair[KnowledgeURI, Concept] => this.generalisations.frames.contains(uriConcept._1)
    }
    commonGeneralisations.size > 0
  }

  /**
   * Returns true if current Concept has at least one same parent with specified.
   * @param parent Concept to compare with          PHRASES
   * @return Boolean true if that has same parent.
   */
  def hasGeneralisation(parent: Concept): Boolean = {
    this.generalisations.frames.contains(parent.uri)
  }

  override def toString: String = this.uri.name

  def getGeneralisationsRec: List[Concept] = {
    val res: List[Concept] = this.generalisations.frames.values.map {
      g: Concept => g.getGeneralisationsRec
    }.toList.flatten
    res
  }

  override def save(kb: KB, parent: KBNodeId, key: String, linkType: String, saved:ListBuffer[String] = new ListBuffer[String]()): Boolean = {
    if (saved.contains(key)) return true;
    var res = kb.saveResource(this, parent, key, linkType)
    saved.append(key)
    //var savedLocal = List[String](key)
    //if (saved!=null) savedLocal::=savedLocal
    for (x: Resource <- generalisations.frames.values.iterator) {
      //if (!savedLocal.contains(x.uri.toString))
      //{
        res &= x.save(kb, this, x.uri.toString, Constant.GENERALISATION_LINK_NAME,saved)
      //  savedLocal::=x.uri.toString
      //}
    }

    for (y: Resource <- specialisations.frames.values.iterator) {
      res &= y.save(kb, this, y.uri.toString, Constant.SPECIALISATION_LINK_NAME,saved)
    }

    for (z: Resource <- phrases.frames.values.iterator) {
      res &= z.save(kb, this, z.uri.toString, Constant.PHRASES_LINK_NAME,saved)
    }

    for (t: ConceptLink <- _conceptLinks) {
      res &= t.source.save(kb, this, t.uri.name, Constant.CONCEPT_LINK_SOURCE_NAME,saved)
      res &= t.destination.save(kb, this, t.uri.name, Constant.CONCEPT_LINK_DESTINATION_NAME,saved)
    }
    res
  }

  override def export: Map[String, String] = {
    super.export + Pair("content", this._content.uri.name)
  }


}


object Concept {

  val log = LoggerFactory.getLogger(this.getClass)

  def apply(phrases: TypedKLine[AnnotatedPhrase], name: String): Concept = {
    new Concept(TypedKLine[Concept]("generalisation"), TypedKLine[Concept]("specialisation"),
      phrases, KnowledgeString(name, name), List[ConceptLink](), KnowledgeURI(name + "Concept"))
  }

  def apply(word: String, name: String): Concept = {
    new Concept(TypedKLine[Concept]("generalisation"),
      TypedKLine[Concept]("specialisation"),
      TypedKLine[AnnotatedPhrase]("sentences", AnnotatedPhrase(word)),
      KnowledgeString(name, name),
      List[ConceptLink](),
      KnowledgeURI(name + "Concept"))
  }

  def apply(name: String): Concept = {
    new Concept(TypedKLine[Concept]("generalisation"),
      TypedKLine[Concept]("specialisation"),
      TypedKLine[AnnotatedPhrase]("sentences"),
      KnowledgeString(name, name),
      List[ConceptLink](),
      KnowledgeURI(name + "Concept"))
  }

  def apply(name: String, probability: Probability): Concept = {
    new Concept(TypedKLine[Concept]("generalisation"),
      TypedKLine[Concept]("specialisation"),
      TypedKLine[AnnotatedPhrase]("sentences"),
      KnowledgeString(name, name),
      List[ConceptLink](),
      KnowledgeURI(name + "Concept"),
      probability)
  }

  def createSubConcept(parent: Concept, name: String): Concept = {
    val it = new Concept(TypedKLine[Concept]("generalisations", parent), TypedKLine[Concept]("specialisations"),
      TypedKLine[AnnotatedPhrase]("sentences"),
      KnowledgeString(name, name),
      List[ConceptLink](),
      KnowledgeURI(name + "Concept"))
    parent.specialisations = parent.specialisations + (it.uri -> it)
    it
  }

  def createInstanceConcept(parent: Concept): Concept = {
    val name = parent.uri.name + "&ID=" + Random.nextInt(Constant.INSTANCE_ID_RANDOM_SEED)
    val it = new Concept(TypedKLine[Concept]("generalisations", parent), TypedKLine[Concept]("specialisations"),
      TypedKLine[AnnotatedPhrase]("sentences"),
      KnowledgeString(name, name),
      List[ConceptLink](),
      KnowledgeURI(name + "Concept"))
    parent.specialisations = parent.specialisations + (it.uri -> it)
    it
  }

  def createInstanceConcept(parent: Concept, content: String): Concept = {
    val name = parent.uri.name + "&ID=" + Random.nextInt(Constant.INSTANCE_ID_RANDOM_SEED)
    val it = new Concept(TypedKLine[Concept]("generalisations", parent), TypedKLine[Concept]("specialisations"),
      TypedKLine[AnnotatedPhrase]("sentences"),
      KnowledgeString(content, content),
      List[ConceptLink](),
      KnowledgeURI(name + "Concept"))
    parent.specialisations = parent.specialisations + (it.uri -> it)
    it
  }

  def load(kb: KB, parentId: KBNodeId, key: String, linkType: String): Concept = {
    val selfMap = kb.loadChild(parentId, key, linkType)
    if (selfMap.isEmpty) {
      log.error("Concept not loaded for link {}/{} for {}", List(key, linkType, parentId.toString))
      //throw new UnexpectedException("Concept not loaded for link " + key + "/" + linkType + " for " + parentId.toString)
      return null
    }

    val ID = new KBNodeId(selfMap)

    def oneList(items: Map[String, Map[String, String]],linkName:String): Map[KnowledgeURI, Concept] = {
      items.keys.foldLeft(Map[KnowledgeURI, Concept]()) {
        (acc, uri) => acc + Pair(KnowledgeURI(uri,true), Concept.load(kb,ID,uri,linkName))
      }
    }

    def oneListPhrases(items: Map[String, Map[String, String]]): Map[KnowledgeURI, AnnotatedPhrase] = {
      items.keys.foldLeft(Map[KnowledgeURI, AnnotatedPhrase]()) {
        (acc, uri) => acc + Pair(KnowledgeURI(uri,true), AnnotatedPhrase.load(kb, ID, uri, Constant.SENTENCES_LINK_NAME))
      }
    }

    val generalisation =
      TypedKLine[Concept](
        "generalisation",
        oneList(kb.loadChildrenMap(ID, Constant.GENERALISATION_LINK_NAME), Constant.GENERALISATION_LINK_NAME)
      )

    val specialisation =
      TypedKLine[Concept](
        "specialisation",
        oneList(kb.loadChildrenMap(ID, Constant.SPECIALISATION_LINK_NAME), Constant.SPECIALISATION_LINK_NAME)
      )

    val sentences =
      TypedKLine[AnnotatedPhrase](
        "specialisation",
        oneListPhrases(kb.loadChildrenMap(ID, Constant.SENTENCES_LINK_NAME))
      )

    val name = selfMap.get("content") match {
      case Some(x) => x
      case None => {
        log.error("Concept without content")
        ""
      }
    }

    val linksSourceMap = kb.loadChildrenMap(ID, Constant.CONCEPT_LINK_SOURCE_NAME)
    val linksDestinationMap = kb.loadChildrenMap(ID, Constant.CONCEPT_LINK_SOURCE_NAME)
    val conceptLinkList: List[ConceptLink] =
      linksSourceMap.keys.foldLeft(List[ConceptLink]()) {
        (acc, uri) => ConceptLink(new Concept(linksSourceMap(uri)), new Concept(linksDestinationMap(uri)), uri) :: acc
      }

    val res = new Concept(generalisation,
      specialisation,
      sentences,
      KnowledgeString(name, name),
      conceptLinkList,
      new KnowledgeURI(selfMap),
      new Probability(selfMap)
    )

    KBMap.register(res, ID.ID)

    res
  }

  /**
   * Copies generalisations, specialisations, phrases, links from source concept to destination concept.
   * @param source to from copy generalisations, specialisations, phrases, links.
   * @param destination to copy to generalisations, specialisations, phrases, links.
   * @return updated destination Concept.
   */
  def copy(source: Concept, destination: Concept): Concept = {
    /*var _generalisations: TypedKLine[Concept],
    var _specialisations: TypedKLine[Concept],
    var _phrases: TypedKLine[AnnotatedPhrase],
    __content: Resource,
    var _conceptLinks: List[ConceptLink],*/
    destination.generalisations = source.generalisations
    destination.specialisations = source.specialisations
    destination.phrases = source.phrases
    destination.links = source.links
    destination
  }
}
