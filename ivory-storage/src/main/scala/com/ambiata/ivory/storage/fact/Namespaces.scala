package com.ambiata.ivory.storage.fact

import com.ambiata.ivory.core._
import com.ambiata.poacher.mr.MrContext
import com.ambiata.poacher.hdfs.Hdfs
import com.ambiata.mundane.io.BytesQuantity
import com.ambiata.mundane.io.MemoryConversions._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputSplit
import scalaz.{Name =>_,_}, Scalaz._


object Namespaces {
  /**
   * @return the list of namespaces for a given factset and their corresponding sizes
   */
  def namespaceSizes(factsetPath: Path): Hdfs[List[(Namespace, BytesQuantity)]] = for {
    dir <- Hdfs.isDirectory(factsetPath)
    _   <- Hdfs.unless(dir)(Hdfs.fail(s"Invalid file $factsetPath for namespaces - must be a directory"))
    ns  <- Hdfs.childrenSizes(factsetPath)
      .flatMap(_.traverseU { case (n, q) => Hdfs.ok(List(n)).filterHidden.map(_.map(_ -> q))}).map(_.flatten)
      .map(_.map { case (n, q) => (Namespace.fromPathNamespace(n), q) })
  } yield ns

  def namespaceSizesSingle(factsetPath: Path, namespace: Namespace): Hdfs[(Namespace, BytesQuantity)] =
    Hdfs.totalSize(factsetPath).map(namespace ->)

  /* Return the size of specific namespaces/factsets */
  def allNamespaceSizes(repository: HdfsRepository, namespaces: List[Namespace], factsets: List[FactsetId]): Hdfs[List[(Namespace, BytesQuantity)]] = {
    namespaces.flatMap(ns => factsets.map(ns ->)).traverse {
      case (ns, fsid) => namespaceSizesSingle(repository.toIvoryLocation(Repository.namespace(fsid, ns)).toHdfsPath, ns)
    }.map(sum).map(_.toList)
  }

  def sum(sizes: List[(Namespace, BytesQuantity)]): Map[Namespace, BytesQuantity] =
    sizes.foldLeft(Map[Namespace, BytesQuantity]()) {
      case (k, v) => k + (v._1 -> implicitly[Numeric[BytesQuantity]].mkNumericOps(k.getOrElse(v._1, 0.mb)).+(v._2))
   }

  def fromSnapshotPath(path: Path): Namespace = {
    Namespace.nameFromStringDisjunction(path.getParent.getName) match {
      case scalaz.\/-(n) => n
      case scalaz.-\/(e) => Crash.error(Crash.DataIntegrity, s"Can not parse snapshot namespace from path '${path}' - ${e}")
    }
  }
}
