package tu.dataservice.knowledgebaseserver

import org.neo4j.kernel.EmbeddedGraphDatabase
import org.neo4j.graphdb.{Transaction, Node, GraphDatabaseService}
import tu.model.knowledge.training.Goal
import tu.model.knowledge.Resource
import org.neo4j.graphdb.index.Index

object N4JKB {
  val defaultFilename = java.lang.System.getProperty("user.home") + "/tu_kb"
  val keyField = "key"
  private var inited = false
  private var _GraphDb: EmbeddedGraphDatabase = _
  private var _nodeIndex: IndexedSeq[Node] = _

  def apply(): EmbeddedGraphDatabase = {
    if (!inited) {
      _GraphDb = new EmbeddedGraphDatabase(defaultFilename)
      ShutdownHook(_GraphDb.shutdown())
      //TODO: _nodeIndex = _GraphDb.index().forNodes( "nodes" );
      inited = true
    }

    _GraphDb
  }

  private var goalIndex:Index[Node] = _GraphDb.index().forNodes( Goal.getClass.getName )

  def init(): Boolean =
  {
    //_GraphDb.getReferenceNode.createRelationshipTo(usersReferenceNode, RelTypes.USERS_REFERENCE );

    if (goalIndex.ensuring(true) != null)
    {
      addIndexedNode(Goal("ProcessIncident"), goalIndex)
    }

    true
  }

  def goals = {goalIndex.ensuring(true)}
    //List(Goal("ProcessIncident"), Goal("ClassifyIncident"), Goal("GetMostProbableAction"), Goal("SearchSolution"))

  private def addIndexedNode(resource:Resource,  index:Index[Node]):Option[Node] = addIndexedNode(resource, resource.uri.name, index)

  private def addIndexedNode(resource:Resource, key:String,  index:Index[Node]):Option[Node] =
  {
    val tx:Transaction = _GraphDb.beginTx()
    try{
      var node:Node = _GraphDb.createNode()
      node.setProperty("type", resource.getClass.getName)
      node.setProperty("name", resource.uri.name)
      index.add( node, "key", key );
      tx.success()
      return Option(node)
    }
    finally
    {tx.finish()}
    return None
  }

  def addIndexedNode(key: String): Node = {
    var node: Node = _GraphDb.createNode();
    node.setProperty(keyField, key);
    //TODO: _nodeIndex.add( node, keyField , key );
    node;
  }

}





/**
 * from https://issues.scala-lang.org/browse/SI-4200?page=com.atlassian.jira.plugin.system.issuetabpanels:all-tabpanel
 * by Christian Krause
 */

/**
 * A virtual machine shutdown hook.
 *
 * Registered shutdown hooks are executed when the virtual machine
 * terminates normally. For more information read the documentation
 * of Runtime. Constructor creates an unregistered instance
 * @param name the shutdown hook's name (for e.g. logging purposes)
 * @param body code to execute when the shutdown hook is executed
 */
final class ShutdownHook(
                          val name: String,
                          private[this] val body: => Unit) {

  def this(body: => Unit) = this("", body)

  private[this] val hook = new Thread(new Runnable() { def run { body } }, name)

  /** Registers this as a virtual machine shutdown hook. */
  def register() = {
    Runtime.getRuntime().addShutdownHook(hook)
    this
  }

  /** Deregisters this from the virtual machine. */
  def deregister() = Runtime.getRuntime.removeShutdownHook(hook)

  override def toString = "ShutdownHook("+name+")"
}

/** Factory for ShutdownHook instances. */
final object ShutdownHook {
  /** Creates a registered instance. */
  def apply(name: String, body: => Unit) = new ShutdownHook(name, body).register()

  /** Creates a registered instance. */
  def apply(body: => Unit) = new ShutdownHook("", body).register()
}