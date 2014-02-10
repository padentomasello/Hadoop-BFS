/*
 *
 * CS61C Spring 2013 Project 2: Small World
 *
 *
 * REMINDERS: 
 *
 * 
 *
 */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.Math;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;


public class SmallWorld {
    // Maximum depth for any breadth-first search
    public static final int MAX_ITERATIONS = 20;

    // Example writable type
    public static class EValue implements Writable {

        public int exampleInt; 
        public long[] exampleLongArray; 

        public EValue(int exampleInt, long[] exampleLongArray) {
            this.exampleInt = exampleInt;
            this.exampleLongArray = exampleLongArray;
        }

        public EValue() {
        }

        // Serializes object - needed for Writable
        public void write(DataOutput out) throws IOException {
            out.writeInt(exampleInt);

            
            int length = 0;

            if (exampleLongArray != null){
                length = exampleLongArray.length;
            }

            // always write the length, since we need to know
            // even when it's zero
            out.writeInt(length);

            // now write each long in the array
            for (int i = 0; i < length; i++){
                out.writeLong(exampleLongArray[i]);
            }
        }

        // Deserializes object - needed for Writable
        public void readFields(DataInput in) throws IOException {
            //reading an int from the serialized object
            exampleInt = in.readInt();

            //reading length from the serialized object
            int length = in.readInt();

            //rebuilding the array from the serialized object
            exampleLongArray = new long[length];
            
            for(int i = 0; i < length; i++){
                exampleLongArray[i] = in.readLong();
            }

        }

        public String toString() {
            return new String();
        }

    }


    /* The first mapper. Part of the graph loading process, currently just an 
     * identity function.*/
    public static class LoaderMap extends Mapper<LongWritable, LongWritable, 
        LongWritable, LongWritable> {

        @Override
        public void map(LongWritable key, LongWritable value, Context context)
                throws IOException, InterruptedException {

            //  getting value passed from main
            int inputValue = Integer.parseInt(context.getConfiguration().get("inputValue"));


            context.write(key, value);
        }
    }


    /* The first reducer. This is also currently an identity function  
     */
    public static class LoaderReduce extends Reducer<LongWritable, LongWritable, 
        LongWritable, LongWritable> {

        public long denom;

        public void reduce(LongWritable key, Iterable<LongWritable> values, 
            Context context) throws IOException, InterruptedException {
            // We grab the denom field from context: 
            denom = Long.parseLong(context.getConfiguration().get("denom"));


            //  iterating through an Iterable
            for (LongWritable value : values){            
                context.write(key, value);
            }
        }

    }

    public static void main(String[] rawArgs) throws Exception {
        GenericOptionsParser parser = new GenericOptionsParser(rawArgs);
        Configuration conf = parser.getConfiguration();
        String[] args = parser.getRemainingArgs();

        // Pass in denom command line arg:
        conf.set("denom", args[2]);

        conf.set("inputValue", (new Integer(5)).toString());

        // Setting up mapreduce job to load in graph
        Job job = new Job(conf, "load graph");

        job.setJarByClass(SmallWorld.class);

        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(LongWritable.class);
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(LongWritable.class);

        job.setMapperClass(LoaderMap.class);
        job.setReducerClass(LoaderReduce.class);

        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        // Input from command-line argument, output to predictable place
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path("bfs-0-out"));

        // Actually starts job, and waits for it to finish
        job.waitForCompletion(true);

        // Repeats your BFS mapreduce
        int i = 0;
        while (i < MAX_ITERATIONS) {
            job = new Job(conf, "bfs" + i);
            job.setJarByClass(SmallWorld.class);

            job.setMapOutputKeyClass(LongWritable.class);
            job.setMapOutputValueClass(LongWritable.class);
            job.setOutputKeyClass(LongWritable.class);
            job.setOutputValueClass(LongWritable.class);

            job.setMapperClass(Mapper.class); // currently the default Mapper
            job.setReducerClass(Reducer.class); // currently the default Reducer

            job.setInputFormatClass(SequenceFileInputFormat.class);
            job.setOutputFormatClass(SequenceFileOutputFormat.class);

            // Notice how each mapreduce job gets gets its own output dir
            FileInputFormat.addInputPath(job, new Path("bfs-" + i + "-out"));
            FileOutputFormat.setOutputPath(job, new Path("bfs-"+ (i+1) +"-out"));

            job.waitForCompletion(true);
            i++;
        }

        // Mapreduce config for histogram computation
        job = new Job(conf, "hist");
        job.setJarByClass(SmallWorld.class);

        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(LongWritable.class);

        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(LongWritable.class);

        job.setMapperClass(Mapper.class); // currently the default Mapper
        job.setReducerClass(Reducer.class); // currently the default Reducer

        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path("bfs-"+ i +"-out"));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.waitForCompletion(true);
    }
}
