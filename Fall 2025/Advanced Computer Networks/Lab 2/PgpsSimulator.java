import java.io.*;
import java.util.*;

/**
 * A simple PGPS Packet Scheduler Simulator
 */
public class PgpsSimulator {
    static final double EPS = 1e-12; // to avoid precision issues

    /**
     * Represents one packet in the system
     */
    static class Packet {
        // flow this packet belongs to
        int flowID;
        // arrival time
        double arrivalTime;
        // packet length
        double length;
        /**
         * this property `index` is used as just last resort to break ties when sorting
         * packets. it is the index of the packet in the original input file.
         */
        int index;
        // virtual start time
        double vStartTime;
        // virtual finishTime time
        double vFinishTime;

        public Packet(int flowID, double arrivalTime, double length, int index) {
            this.flowID = flowID;
            this.arrivalTime = arrivalTime;
            this.length = length;
            this.index = index;
            // init virtual times to zero
            this.vStartTime = 0.0;
            this.vFinishTime = 0.0;
        }

        @Override
        public String toString() {
            return "Packet{flowId=" + flowID
                    + ", arrTime=" + arrivalTime
                    + ", length=" + length
                    + ", virtualStarTime=" + vStartTime
                    + ", virtualFinishTime=" + vFinishTime + "}";
        }
    }

    /** Event type for scheduling */
    static class Event {
        // type of an event. e.g. "pgpsDeparture"
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

        @Override
        public String toString() {
            return "Event{type=" + type
                    + ", t=" + time
                    + ", p=" + packet + "}";
        }
    }

    /**
     * reusable comparator for packets sorting and priority queue
     */
    private static final Comparator<Packet> PACKET_COMPARATOR = (a, b) -> {
        if (a.vFinishTime < b.vFinishTime - EPS)
            return -1;
        if (a.vFinishTime > b.vFinishTime + EPS)
            return 1;
        if (a.flowID < b.flowID)
            return -1;
        if (a.flowID > b.flowID)
            return 1;
        return Integer.compare(a.index, b.index);
    };

    public static void main(String[] args) throws Exception {
        // the input and output file names
        final String inputFileName = "flows.txt";
        final String outputFileName = "flowout.txt";
        // just a flag to log to console or not
        final boolean logToConsole = Arrays.asList(args).contains("--verbose");
        // reader and writer
        Scanner inputFileReader = new Scanner(new File(inputFileName));
        FileWriter outputFileWriter = new FileWriter(outputFileName);

        int nFlows = inputFileReader.nextInt();
        double[] weight = new double[nFlows + 1];

        // starting from index 1 for convenience only ;)
        for (int index = 1; index <= nFlows; index++) {
            weight[index] = inputFileReader.nextDouble();
        }

        int nPackets = inputFileReader.nextInt();
        List<Packet> packets = new ArrayList<>();

        for (int index = 0; index < nPackets; index++) {
            double arrivalTime = inputFileReader.nextDouble();
            int fid = inputFileReader.nextInt();
            double len = inputFileReader.nextDouble();
            packets.add(new Packet(fid, arrivalTime, len, index));
        }
        inputFileReader.close();

        /**
         * Just printing packets to console and output file.
         */
        if (logToConsole) {
            System.out.println("nFlows = " + nFlows);
        }
        outputFileWriter.write("nFlows = " + nFlows + '\n');
        if (logToConsole) {
            System.out.println("nPackets = " + nPackets);
        }
        outputFileWriter.write("nPackets = " + nPackets + '\n');
        for (Packet packet : packets) {
            if (logToConsole) {
                System.out.println("Packet arrTime " + packet.arrivalTime
                        + " flow id " + packet.flowID
                        + " w " + weight[packet.flowID]
                        + " packet length " + packet.length);
            }
            outputFileWriter.write("Packet arrTime " + packet.arrivalTime
                    + " flow id " + packet.flowID
                    + " w " + weight[packet.flowID]
                    + " packet length " + packet.length + '\n');
        }

        // Do NOT sort packets by virtual finish time before simulation
        // Only sort by input order (index) to preserve arrival order
        packets.sort(Comparator.comparingInt(p -> p.index));

        double[] lastVFinishTime = new double[nFlows + 1];
        double virtualTime = 0.0;
        LinkedList<Packet> waitingListOfPackets = new LinkedList<>(packets);
        // Use a priority queue with correct comparator for PGPS departures
        PriorityQueue<Packet> queue = new PriorityQueue<>(PACKET_COMPARATOR);
        List<Event> events = new ArrayList<>();
        double currentTime = 0.0;

        while (!waitingListOfPackets.isEmpty() || !queue.isEmpty()) {
            // Add all packets that have arrived by currentTime
            while (!waitingListOfPackets.isEmpty() && waitingListOfPackets.peek().arrivalTime <= currentTime + EPS) {
                Packet packet = waitingListOfPackets.poll();
                // Assign virtual start/finish times
                packet.vStartTime = Math.max(lastVFinishTime[packet.flowID], virtualTime);
                packet.vFinishTime = packet.vStartTime + (packet.length / weight[packet.flowID]);
                lastVFinishTime[packet.flowID] = packet.vFinishTime;
                queue.add(packet);
            }

            if (!queue.isEmpty()) {
                // Always pick the packet with the smallest virtual finish time (ties by input
                // order)
                Packet packet = queue.poll();
                double startTime = Math.max(currentTime, packet.arrivalTime);
                double finishTime = startTime + packet.length;
                virtualTime = packet.vFinishTime;
                currentTime = finishTime;

                Event e = new Event("pgpsDeparture", finishTime, packet);
                events.add(e);
                if (logToConsole) {
                    System.out.println(e);
                }
                outputFileWriter.write(e.toString() + "\n");
            } else {
                // Advance time to next packet arrival if queue is empty
                if (!waitingListOfPackets.isEmpty()) {
                    currentTime = Math.max(currentTime, waitingListOfPackets.peek().arrivalTime);
                }
            }
        }

        outputFileWriter.close();
    }
}
