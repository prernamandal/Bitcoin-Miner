import scala.util.Random
import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.actor.actorRef2Scala
import akka.dispatch.ExecutionContexts.global
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import scala.io.Source.fromFile
import java.security.MessageDigest
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import akka.dispatch.ExecutionContexts._

case class StartBitcoinMiningInLocal(numOfLeadingZeros: Integer,gatorid:String)
case class ConsolidateCoins(tries: Integer,bitcoin: String, hexvalue: String)

object BitcoinMasterApp extends App {
 
  // import akka.util.Timeout
  // import scala.concurrent.duration._
  // import akka.pattern.ask
  // import akka.dispatch.ExecutionContexts._
  override def main(args: Array[String]) {
  implicit val system = ActorSystem("BitcoinMinerLocalSystem")
    var numOfLeadingZeros:Int = args(0).toInt
    var launchRemote = args(1)
    
    val actor = system.actorOf(Props(new BitcoinLocalMaster("prernamandal",numOfLeadingZeros,launchRemote)))
    val act2 = system.actorOf(Props[BitcoinLocalCollector], name = "BitcoinLocalCollector")
    implicit val timeout = Timeout(25 seconds) //???
    val future = actor ? StartBitcoinMining()
                                                   
}
}

class BitcoinLocalCollector extends Actor {
  def receive = {
    case msg: String => 
        println("\n Bitcoins mined from Remote workers \n\n"+ msg)

    // case GiveWorkToRemote1() =>{
    //   implicit val system = ActorSystem("LocalSystem")
    //   val actorRemoteRefer = system.actorOf(Props(new MineBitcoins("prernamandal",k)))
    //   val future = actorRemoteRefer ? GiveWorkToRemote2()
    // }
        
  }
}

case class StartBitcoinMining()
case class WorkerReportToMaster()
class BitcoinLocalMaster(gatorid: String,numOfLeadingZeros: Integer,launchRemote:String) extends Actor {
  private var tries = 0
  private var countOfWorker = 0
  var remoteMinerRef = context.actorFor("akka.tcp://BitcoinMinerRemoteSystem@192.168.0.13:2552/user/BitcoinRemoteMiner")
 
  val processes = Runtime.getRuntime().availableProcessors();
  //val actorcount=(10*processes)/2
  val actorcount=  5 * processes
  //println("actorCount is" + actorcount)
  // LOGIC TO GET STRING PREFIXED WITH GATORID, APPEND SOME STRINGS, DIVIDE AND ASSIGN IT TO WORKERS
  
  
  def receive = {
    
    case msg : String => 
        println(s": '$msg'")
        
    case StartBitcoinMining() => {
  
        val actor = context.actorOf(Props[BitcoinLocalMiner].withRouter(RoundRobinRouter(actorcount)))
        //println("Search space created.....Now mining Bitcoins...!!\n\n")
        
        println("Now mining being done by local workers ")
        for( workerCount <- 1 to actorcount) {
        //println("giving work to local workers")
        var gatorid = "prernamandal"
        gatorid = gatorid + "localw" + workerCount  
        actor ! StartBitcoinMiningInLocal(numOfLeadingZeros,gatorid)
        }
        if(launchRemote == "launchRemoteToo"){
        println("giving work to remote workers")
        remoteMinerRef ! numOfLeadingZeros
        }
      }

    

    case WorkerReportToMaster() => {
        countOfWorker = countOfWorker + 1
        //if(countOfWorker >= actorcount && launchRemote != "launchRemoteToo"){
        if(countOfWorker >= actorcount){  
            println("Done with all workers" + countOfWorker + "-----" + actorcount + " so shutting down")
            context.system.shutdown()
        }
    }
    
    case ConsolidateCoins(tries,bitcoin,hexvalue/*BITCOINS RECEIVED FROM WORKERS*/) => {
      println("Bitcoins are here: " + bitcoin + "     "+ hexvalue)
   
      
    }
    
    //case _ => println("Message not recognized!")
  }
}

 
class BitcoinLocalMiner extends Actor {
  var tries=0
  def calculateCoinsFromLocalWorker(numOfLeadingZeros: Int , hashStringConstant:String):String = {
        var hashStringToReturn = ""
            var zeroString:String = ""
            for( stringLengthIterator <- 1 to numOfLeadingZeros) {
              zeroString = zeroString.concat("0");
            }
            //println(zeroString)
            var bitCoinObtained = 0;
            val timeStart:Long = System.currentTimeMillis
            
           var r:String = Random.alphanumeric.take(6).mkString
            var proofOfWork:Integer=0

                 while(System.currentTimeMillis()-timeStart<2700) {
                  
                     var hashString = hashStringConstant + r+ proofOfWork
                     var sha = MessageDigest.getInstance("SHA-256")
                      
                     def cryptoCurrencyFinder(s: String): String = {
                        sha.digest(s.getBytes)
                          .foldLeft("")((s: String, b: Byte) => s +
                            Character.forDigit((b & 0xf0) >> 4, 16) +
                            Character.forDigit(b & 0x0f, 16))
                      }
                     
                      var hashed = cryptoCurrencyFinder(hashString.toString)
                      proofOfWork+=1
                      
                      if (hashed.substring(0,numOfLeadingZeros) == zeroString)
                      {   //println ("Match found in local worker")
                          //println( hashString+"\t"+hashed )
                          bitCoinObtained = bitCoinObtained + 1
                          var hashStringToReturn1 = hashString.concat("  : "+ hashed)
                          hashStringToReturn = hashStringToReturn.concat(hashStringToReturn1)

                          sender ! ConsolidateCoins(tries,hashString,hashed)

                      }else{
                        
                      }
                 }     

                 sender ! WorkerReportToMaster()

                 if(hashStringToReturn == ""){
                  hashStringToReturn = "No match in local worker"                 
                 }
                 var timeEnd: Long = System.currentTimeMillis
                 var timeTakenToMine = timeEnd - timeStart
                 //println("Time taken to mine the coin in local worker" + timeTakenToMine)
                 println("No. of bitcoins mines in local worker" + bitCoinObtained)
                 bitCoinObtained.toString + "bye bye local worker"

       }
      def receive = {
        case StartBitcoinMiningInLocal(numOfLeadingZeros,gatorid) => {       
          //LOGIC TO MINE BITCOINS FROM STRING RECEIVED. - IMPLEMENTED
            //println("numOfLeadingZeros= in local worker-" + numOfLeadingZeros + " id= in local worker - " + gatorid)
            var bitcoinQueue:String = calculateCoinsFromLocalWorker(numOfLeadingZeros,gatorid)
            //println(" local actor going back" + bitcoinQueue)
            
               }
        case _ => println("Uncaught Message available")
      }
} 


 


