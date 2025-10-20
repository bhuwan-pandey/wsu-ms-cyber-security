
import java.io.*;
import java.util.*;

public class Lab2U01155118I {
    static final double EPS = 1e-9;
    static final double MINW = 1e-18;
    /**
     * creating `Flow` class and storing flows just to mimic real scenario and take
     * advantage of OOP. :)
     */
    static Flow[] flows = null;
    // the same with the `Clock` class.
    static Clock clock = new Clock(); // our clock instance ;)

    /**
     * Self explanatory time class
     */
    static class Clock {
        double virtualTime = 0.0;
        double realTime = 0.0;
    }

    /** Event type for scheduling */
    static class Event {
        /**
         * type of an event. "pgpsDeparture" for now. but it could be `pgpsArrival` too.
         * and i would have to compile event to `eligibleDepartures` and
         * `futureDepartures` queues. and that would be a bit messy (at least for now).
         */
        String type;
        // event time (departure time)
        double time;
        // packet related to this event
        Packet packet;

        public Event(String type, double time, Packet packet) {
            this.type = type;
            this.time = time;
            this.packet = packet;
        }
    }

    static class Flow {
        int id;
        double weight;
        double lastVirtualFinish = 0.0;
        int numberOfBackloggedPackets = 0; // number of packets currently admitted (for backlog)

        Flow(int id, double weight) {
            this.id = id;
            this.weight = weight;
        }

        boolean isBacklogged() {
            return numberOfBackloggedPackets > 0;
        }
    }

    static class Packet {
        double arrivalTime;
        int flowID;
        double length;
        double virtualStartTime;
        double virtualFinishTime;
        int index; // indicates order in input file
        double serviceVirtualStartTime = Double.NaN;

        Packet(int flowID, double arrivalTime, double length, int index) {
            this.flowID = flowID;
            this.arrivalTime = arrivalTime;
            this.length = length;
            this.index = index;
            this.virtualStartTime = 0.0;
            this.virtualFinishTime = 0.0;
        }

        public Flow getFlow() {
            return flows[flowID];
        }

        public String printArrivalInfo() {
            return String.format("Packet arrTime %.1f flow id %d w %.1f packet length %.1f%n",
                    this.arrivalTime, this.flowID, this.getFlow().weight, this.length);
        }

        public String printPacketInfo() {
            return String.format(
                    "Packet{flowId=%d, arrTime=%.1f, length=%.1f, virtualStarTime=%.1f, virtualFinishTime=%.1f}",
                    flowID, arrivalTime, length, virtualStartTime, virtualFinishTime);
        }

        public String printEventInfo() {
            /**
             * this could have been done via Event class but the `eligibleDepartures` and
             * `futureDepartures` queues only hold `Packet` instances for easy scheduling.
             * even though this makes me feel little off, like design wise, but it is how
             * it is for now :|
             */
            return String.format("Event{type=pgpsDeparture, t=%.1f, p=%s}%n", clock.realTime,
                    this.printPacketInfo());
        }
    }

    // self explanatory function to compute total backlog weight
    static double getTotalBackloggedWeight() {
        double weight = 0.0;
        for (Flow flow : flows)
            if (flow != null // just to avoid first null entry. remember ? ;)
                    && flow.lastVirtualFinish > clock.virtualTime + EPS)
                weight += flow.weight;
        return weight;
    }

    // set virtual time and real time using backlog weight.
    static void changeClockTime(double newRealTime, double weight) {
        if (newRealTime <= clock.realTime + EPS) {
            clock.realTime = Math.max(clock.realTime, newRealTime);
            return;
        }
        double deltaOfRealTime = newRealTime - clock.realTime;
        if (weight > 0.0) {
            clock.virtualTime += deltaOfRealTime / Math.max(MINW, weight);
        }
        clock.realTime = newRealTime;
    }

    public static void main(String[] args) throws Exception {
        String inputFileName = "flows.txt";
        String outputFileName = "flowout.txt";
        boolean logToConsole = Arrays.asList(args).contains("--verbose");
        // reader and writer
        Scanner inputFileReader = new Scanner(new File(inputFileName));
        // `PrintWriter` makes it easy to write formatted output :)
        PrintWriter outputFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputFileName)));

        List<Event> arrivalEvents = new ArrayList<>();

        int numberOfFlows = inputFileReader.nextInt();
        flows = new Flow[numberOfFlows + 1];

        // starting from index 1 for convenience only ;)
        for (int flowID = 1; flowID <= numberOfFlows; flowID++) {
            double weight = inputFileReader.nextDouble();
            // create flow
            flows[flowID] = new Flow(flowID, weight);
        }

        int numberOfPackets = inputFileReader.nextInt();
        outputFileWriter.println("numberOfFlows = " + numberOfFlows);
        outputFileWriter.println("numberOfPackets = " + numberOfPackets);
        for (int index = 0; index < numberOfPackets; index++) {
            double arrivalTime = inputFileReader.nextDouble();
            int flowID = inputFileReader.nextInt();
            double packetLength = inputFileReader.nextDouble();
            Packet packet = new Packet(flowID, arrivalTime, packetLength, index);
            Event arrivalEvent = new Event("pgpsArrival", arrivalTime, packet);
            /**
             * a packet (added) in the flow means arrival event for that packet
             */
            arrivalEvents.add(new Event("pgpsArrival", arrivalTime, packet));
            outputFileWriter.printf(arrivalEvent.packet.printArrivalInfo());
        }
        inputFileReader.close();

        // sort arrivalEvents by arrival time.
        arrivalEvents.sort(Comparator.comparingDouble(arrivalEvent -> arrivalEvent.packet.arrivalTime));

        // priority queues for eligible and future departures
        PriorityQueue<Packet> futureDepartures = new PriorityQueue<>(
                Comparator.comparingDouble(p -> p.virtualStartTime));
        PriorityQueue<Packet> eligibleDepartures = new PriorityQueue<>(Comparator
                .comparingDouble((Packet p) -> p.virtualFinishTime)
                .thenComparingDouble(p -> p.virtualStartTime)
                .thenComparingDouble(p -> p.arrivalTime)
                .thenComparingInt(p -> p.index));

        int numberOfDepartedPackets = 0; // determines how many packets have departed
        Packet packetInProgress = null; // currently in-service (being processed) packet (null if none)

        /**
         * The main simulation loop till all packets depart
         */
        while (numberOfDepartedPackets < numberOfPackets) {
            /**
             * loop IF any arrival events are pending,
             * AND
             * if the next arrival's time is earlier than current real time, we need to
             * change to that arrival time first.
             */
            while (!arrivalEvents.isEmpty() &&
                    arrivalEvents.get(0).packet.arrivalTime <= clock.realTime + EPS) {
                Event arrivalEvent = arrivalEvents.remove(0);
                Packet arrivedPacket = arrivalEvent.packet;

                // change virtual time to the exact arrival instant AND must compute weight.
                double totalBackloggedWeight = getTotalBackloggedWeight();
                // if there is a packet in service, include its flow's weight for the interval
                if (packetInProgress != null) {
                    if (!(packetInProgress.getFlow().lastVirtualFinish > clock.virtualTime + EPS)
                            && packetInProgress.virtualFinishTime > clock.virtualTime + EPS) {
                        totalBackloggedWeight += packetInProgress.getFlow().weight;
                    }
                }
                // change to arrival instant
                changeClockTime(arrivedPacket.arrivalTime, totalBackloggedWeight);

                // compute times
                arrivedPacket.virtualStartTime = Math.max(arrivedPacket.getFlow().lastVirtualFinish, clock.virtualTime);
                arrivedPacket.virtualFinishTime = arrivedPacket.virtualStartTime + (arrivedPacket.length / arrivedPacket
                        .getFlow().weight);
                arrivedPacket.getFlow().lastVirtualFinish = arrivedPacket.virtualFinishTime;
                // flow is now backlogged with this new packet
                arrivedPacket.getFlow().numberOfBackloggedPackets++;

                if (arrivedPacket.virtualStartTime <= clock.virtualTime + EPS) { // eligible for now
                    eligibleDepartures.add(arrivedPacket);
                } else { // eligible in future
                    futureDepartures.add(arrivedPacket);
                }

                // if next future departure packet is now eligible, then schedule it
                while (!futureDepartures.isEmpty()
                        && futureDepartures.peek().virtualStartTime <= clock.virtualTime + EPS) {
                    eligibleDepartures.add(futureDepartures.poll());
                }
            }

            // If no packet is in progress.
            if (packetInProgress == null) {
                /**
                 * flag to indicate if we departed/processed any eligible packet iteration.
                 */
                boolean flushed = false;
                while (!eligibleDepartures.isEmpty()
                        && eligibleDepartures.peek().virtualFinishTime <= clock.virtualTime + EPS) {
                    Packet packet = eligibleDepartures.poll();
                    packet.serviceVirtualStartTime = clock.virtualTime;
                    numberOfDepartedPackets++;
                    // output
                    outputFileWriter.printf(packet.printEventInfo());
                    if (logToConsole)
                        System.out.print(packet.printEventInfo());
                    // decrement numberOfBackloggedPackets
                    flows[packet.flowID].numberOfBackloggedPackets--;
                    flushed = true; // packet served
                }
                if (flushed) {
                    // continue loop if a packet is served/departed
                    continue;
                }

                // if still no eligible packets, we need to change time.
                if (eligibleDepartures.isEmpty()) {
                    // compute backlog weight (no packetInProgress)
                    double totalBackloggedWeight = getTotalBackloggedWeight();
                    double nextArrivalTime = arrivalEvents.isEmpty() ? Double.POSITIVE_INFINITY
                            : arrivalEvents.get(0).packet.arrivalTime;
                    double nextVirtualTime = futureDepartures.isEmpty() ? Double.POSITIVE_INFINITY
                            : futureDepartures.peek().virtualStartTime;

                    if (Double.isInfinite(nextArrivalTime) && Double.isInfinite(nextVirtualTime)) {
                        // nothing left
                        break;
                    }

                    /**
                     * compute real time to reach next virtual time given the
                     * `totalBackloggedWeight`.
                     */
                    double boundaryDelta = Double.POSITIVE_INFINITY;
                    if (!Double.isInfinite(nextVirtualTime))
                        boundaryDelta = (nextVirtualTime - clock.virtualTime) * Math.max(MINW, totalBackloggedWeight);
                    if (nextArrivalTime < clock.realTime + boundaryDelta - EPS) {
                        // go to next arrival time
                        changeClockTime(nextArrivalTime, totalBackloggedWeight);
                        // loop will process that arrival at top
                        continue;
                    } else {
                        // virtual to next virtualStartTime (no `realTime`)
                        if (!Double.isInfinite(nextVirtualTime)) {
                            clock.virtualTime = futureDepartures.peek().virtualStartTime;
                            // yup. again !
                            while (!futureDepartures.isEmpty()
                                    && futureDepartures.peek().virtualStartTime <= clock.virtualTime + EPS)
                                eligibleDepartures.add(futureDepartures.poll());
                            continue;
                        } else {
                            // change to next arrival
                            changeClockTime(nextArrivalTime, totalBackloggedWeight);
                            continue;
                        }
                    }
                }
            }

            /**
             * if idle and eligible packets exist, start the one with smallest vrtual finish
             * time.
             */
            if (packetInProgress == null && !eligibleDepartures.isEmpty()) {
                packetInProgress = eligibleDepartures.poll();
                packetInProgress.serviceVirtualStartTime = clock.virtualTime;
            }

            // if in progress, simulate until next arrival or until finish
            if (packetInProgress != null) {
                // compute weight including being-processed flow
                double totalBackloggedWeight = getTotalBackloggedWeight();
                if (!(packetInProgress.getFlow().lastVirtualFinish > clock.virtualTime + EPS)
                        && packetInProgress.virtualFinishTime > clock.virtualTime + EPS) {
                    totalBackloggedWeight += packetInProgress.getFlow().weight;
                }

                // how much virtual-time needed to finish current packet
                double deltaVirtualTime = Math.max(0.0, packetInProgress.virtualFinishTime - clock.virtualTime);
                double timeToFinish = (totalBackloggedWeight > 0.0) ? deltaVirtualTime * Math.max(MINW,
                        totalBackloggedWeight) : Double.POSITIVE_INFINITY;
                double nextArrivalTime = arrivalEvents.isEmpty() ? Double.POSITIVE_INFINITY
                        : arrivalEvents.get(0).packet.arrivalTime;

                if (nextArrivalTime < clock.realTime + timeToFinish - EPS) {
                    // an arrival occurs before finish
                    changeClockTime(nextArrivalTime, totalBackloggedWeight);
                    // loop at top will handle the arrival (we continue without completing)
                    continue;
                } else {
                    // finish before next arrival
                    changeClockTime(clock.realTime + timeToFinish, totalBackloggedWeight);
                    // set virtual time exactly to virtualFinishTime to avoid tiny changes
                    clock.virtualTime = packetInProgress.virtualFinishTime;
                    numberOfDepartedPackets++;
                    outputFileWriter.printf(packetInProgress.printEventInfo());
                    if (logToConsole)
                        System.out
                                .print(packetInProgress.printEventInfo());
                    flows[packetInProgress.flowID].numberOfBackloggedPackets--; // reduce `numberOfBackloggedPackets`
                    packetInProgress = null; // packet done
                    // and again !
                    while (!futureDepartures.isEmpty()
                            && futureDepartures.peek().virtualStartTime <= clock.virtualTime + EPS)
                        eligibleDepartures.add(futureDepartures.poll());
                }
            }
        }

        outputFileWriter.close();
    }
}
