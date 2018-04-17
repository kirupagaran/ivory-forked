package com.ambiata.ivory.core

import com.ambiata.com.amazonaws.services.s3.AmazonS3Client
import com.ambiata.saws.core.Clients
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.compress.CompressionCodec

case class IvoryConfiguration(
    s3Client: AmazonS3Client,
    hdfs: () => Configuration,
    compressionCodec: () => Option[CompressionCodec]) {

  lazy val configuration: Configuration             = hdfs()
  lazy val codec: Option[CompressionCodec]          = compressionCodec()
}

object IvoryConfiguration {
  def fromConfiguration(configuration: Configuration): IvoryConfiguration =
    new IvoryConfiguration(
      s3Client = Clients.s3,
      hdfs = () => configuration,
      compressionCodec = () => None)
}
