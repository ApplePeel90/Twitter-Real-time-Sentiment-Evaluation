import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

object TwitterSentiment {
  def main(args: Array[String]): Unit = {



    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)



    val consumerKey = "V7OMRjIa9lQ8RFXfY3UPfdo0m";
    val consumerSecret = "5bPa97gpgsDrjWlUjAtpmhbUrrECTR8sCpImPwxQRzrtuIy9c4";
    val accessToken = "1249752962173452289-I8w8wCvco75xusV3tSPJIOAwwZYymA";
    val accessTokenSecret = "bQUQMM7yTcuS4ICjlVTsRrCrb7YQ5fXlDRr2c3Ks8qTyP";
//    val Array(consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.take(4)
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)

    val filters = Seq(args(0))
    val topic = args(0)

    val sparkConf = new SparkConf().setAppName("TwitterSentiment_ApplePeel90").setMaster("local[*]")
    val ssc = new StreamingContext(sparkConf,Seconds(5))
    val stream = TwitterUtils.createStream(ssc, None,filters).filter(x=>x.getLang=="en")

    stream.foreachRDD(rdd=>{
      rdd.cache()
      val serializer = "org.apache.kafka.common.serialization.StringSerializer"
      val props = new Properties()
      props.put("bootstrap.servers", "localhost:9092")
      props.put("key.serializer", serializer)
      props.put("value.serializer", serializer)
      val producer = new KafkaProducer[String,String](props)
      rdd.collect().toList.foreach(tweet => {
        val txt = tweet.getText()
        producer.send(new ProducerRecord[String,String](topic,txt,SentimentAnalysis.mainSentiment(txt).toString()))
      })
      producer.close()
    })
    ssc.start()
    ssc.awaitTermination()
  }
}
