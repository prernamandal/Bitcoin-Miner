
import scala.util.Random
import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.actor.actorRef2Scala
import akka.dispatch.ExecutionContexts.global
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import scala.io.Source.fromFile
import java.security.MessageDigest
import akka.actor.{ Address, AddressFromURIString }
import akka.routing.RoundRobinRouter
import akka.actor._
import scala.concurrent.duration
import scala.concurrent.duration._
import java.security.MessageDigest
import util.control.Breaks._

case class StartBitCoinMiningInRemote(k: Integer,gatorid:String)
case class NumberOfTries()
case class WorkerReportToRemoteMaster()


object HelloRemote extends App{
  val system = ActorSystem("BitcoinMinerRemoteSystem")
  val remoteActor = system.actorOf(Props[BitcoinRemoteMiner], name = "BitcoinRemoteMiner")
}

class MineBitcoins extends Actor{
    var remote1 = context.actorFor("akka.tcp://BitcoinMinerLocalSystem@192.168.0.16:2552/user/BitcoinLocalCollector")
    var tries=0
    //Function to calculate the hashing input and hash of it , compare to find bitcoins
    def calculateCoinsFor(numOfLeadingZeros: Int , hashStringConstant:String):String = {
      var hashStringToReturn: String= ""
      var zeroString:String = ""
      for( stringLengthIterator <- 1 to numOfLeadingZeros){
        zeroString = zeroString.concat("0");
      }
    //println(zeroString + "Inside remoteWorker function")
    var bitCoinObtained = 0;
    val timeStart:Long = System.currentTimeMillis
    //println(timeStart)
    var r:String = Random.alphanumeric.take(6).mkString
    var proofOfWork:Integer=0
    while(System.currentTimeMillis()-timeStart<2700){
          var hashString : String = hashStringConstant+r+proofOfWork
          var sha = MessageDigest.getInstance("SHA-256")
          def cryptoCurrencyFinder(s: String): String = {
                sha.digest(s.getBytes)
                          .foldLeft("")((s: String, b: Byte) => s +
                            Character.forDigit((b & 0xf0) >> 4, 16) +
                            Character.forDigit(b & 0x0f, 16))
          }
          var hashed = cryptoCurrencyFinder(hashString.toString)
          proofOfWork+=1
          if (hashed.substring(0,numOfLeadingZeros) == zeroString){   
            bitCoinObtained = bitCoinObtained + 1
            var hashStringToReturn1 = hashString.concat("  : "+ hashed)
            hashStringToReturn = hashStringToReturn.concat(hashStringToReturn1 + "\n");
            }
            else{

            }
    }     
    if(hashStringToReturn == ""){
        hashStringToReturn = "No match inside remoteWorker"                 
    }
                 
                 sender ! WorkerReportToRemoteMaster()
                 var timeEnd: Long = System.currentTimeMillis
                 var timeTakenToMine = timeEnd - timeStart
                 // println("Time taken to mine the coins in remoteWorker" + timeTakenToMine)
                 // println("No. of bitcoins mined by remoteWorker" + bitCoinObtained)
                 // println("hashStringToReturn : "+hashStringToReturn)
                 hashStringToReturn
                 
       }
  def receive = {
    case StartBitCoinMiningInRemote(k,gatoridConcatenatedWithWorker) => {
      
       //println("Remote Actor started to mine..")
      //LOGIC TO MINE BITCOINS FROM STRING RECEIVED. - IMPLEMENTED
        
        //ar bitcoinQueue : String= ""
        
        //println("k=" + k + " id= " + gatoridConcatenatedWithWorker)
        var bitcoinQueue : String = calculateCoinsFor(k,gatoridConcatenatedWithWorker)
       
        remote1 ! bitcoinQueue
           }
    
  }
} 



class BitcoinRemoteMiner extends Actor {
var tries = 0
   val processes = Runtime.getRuntime().availableProcessors();
  val actorcount=5*processes
  private var countOfWorker = 0
  def receive = {
    case msg: Int => {
       //println("BitCoinRemoteMiner received message")
      
        val actor = context.actorOf(Props[MineBitcoins].withRouter(RoundRobinRouter(actorcount)))
       println(msg)
      var k = msg
       

      val gatorid="prernamandal"
      for (workerCount <- 1 to actorcount)
      {
      val gatoridConcatenatedWithWorker = gatorid+"worker"+workerCount
     sender ! (actor ! StartBitCoinMiningInRemote(k,gatoridConcatenatedWithWorker)) 
      }
}
    case WorkerReportToRemoteMaster() => {
        countOfWorker = countOfWorker + 1
        if(countOfWorker >= actorcount){
            //println("Done with all Remoteworkers" + countOfWorker + "-----" + actorcount + " so shutting down")
            context.system.shutdown()
        }
    }
  }
}



