import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class b {

	public static class myMapper extends Mapper<Object, Text, Text, IntWritable> {
		private Text word = new Text();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String url = value.toString();
			URL oracle = new URL(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));

			String inputLine;
			String[] tmp;
			String zipcode;
			int n = 0;
			int start_index = 0;
			int end_index = 0;
			
			while ((inputLine = in.readLine()) != null) {
				n++;
				if (n == 1) continue;
				tmp = inputLine.split(",", 2);
				end_index   = tmp[1].lastIndexOf(',');
				start_index = tmp[1].lastIndexOf(',', end_index - 1) + 1;
				zipcode = tmp[1].substring(start_index, end_index);
				word.set(tmp[1]);
				context.write(word, new IntWritable(Integer.parseInt(zipcode)));
			}
			in.close();
		}
	}

	public static class myCombiner extends Reducer<Text, IntWritable, Text, IntWritable> {
		 
		protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			Set<IntWritable> uniques = new HashSet<IntWritable>();
			for (IntWritable value : values) {
				if (uniques.add(value)) {
					context.write(key, value);
				}
			}
		}
	}
	
	public static class myReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		private IntWritable result = new IntWritable();

		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			int zipcode = 0;
			for (IntWritable val : values) {
				zipcode = val.get();
				if (zipcode < 10030) result.set(zipcode);
			}
			context.write(key, result);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage: wordcount <in> <out>");
			System.exit(2);
		}
		Job job = new Job(conf, "word count");
		job.setJarByClass(b.class);
		job.setMapperClass(myMapper.class);
		job.setCombinerClass(myCombiner.class);
		job.setReducerClass(myReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
