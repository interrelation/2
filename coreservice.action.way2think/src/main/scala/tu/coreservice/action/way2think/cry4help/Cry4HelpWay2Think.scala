package tu.coreservice.action.way2think.cry4help

import tu.coreservice.action.way2think.Way2Think
import tu.model.knowledge.communication.{Response, ContextHelper, Context}
import tu.model.knowledge.{KnowledgeURI, Resource}
import tu.model.knowledge.primitive.KnowledgeString
import tu.coreservice.utilities.LocalizedResources

/**
 * @author max talanov
 *         date 2012-06-25
 *         time: 4:25 PM
 */

class Cry4HelpWay2Think(var _inputContext: Context, _uri: KnowledgeURI)
  extends Way2Think(_uri) {

  def this() = {
    this(ContextHelper.apply(List[Resource](KnowledgeString("", "Cry4HelpMessage")), ""), KnowledgeURI("Cry4HelpMessage"))
  }

  /**
   * Way2Think interface.
   * @param inputContext Context of all inbound parameters.
   * @return outputContext
   */
  def apply(inputContext: Context): Context = {
    this._inputContext = inputContext
    val provider = Cry4HelpProviders.GetProvider()

    provider.showInformation(LocalizedResources.GetString("ErrorOccured"))
    provider.showInformation(inputContext.lastError.toString)
    val userResponseText = provider.askQuestion(LocalizedResources.GetString("ProvideAdditionalInfo"))
    val outputContext: Context = ContextHelper(List[Resource](), this.getClass.getName)
    outputContext.userResponse = Some(new Response(KnowledgeString(userResponseText, "responsetext"), KnowledgeURI("Response")))
    outputContext
  }

  def start() = false

  def stop() = false

  override def toString: String = {
    this.getClass.getName + ":" + _inputContext.toString()
  }
}

object Cry4HelpWay2Think {
  def apply(message: String): Cry4HelpWay2Think = {
    val res = List[Resource](KnowledgeString(message, "Cry4HelpMessage"))
    val context = ContextHelper.apply(res, message)
    val cry4helpWay2Think = new Cry4HelpWay2Think()
    cry4helpWay2Think.apply(context)
    cry4helpWay2Think
  }
}
