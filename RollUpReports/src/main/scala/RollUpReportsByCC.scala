/**
 * Created by Kkusoorkar-MBP15 on 3/18/16.
 */

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SaveMode}


object RollUpReportsByCC {

  def main (args: Array[String]){
    println("Beginning RollUp Reporting By CC...")

    val conf = new SparkConf()
      .setAppName("RollUpReportsByCC")

    val sc = SparkContext.getOrCreate(conf)
    val sqlContext = new HiveContext(sc)

    sqlContext.sql("""CREATE TEMPORARY TABLE temp_transactions
      USING org.apache.spark.sql.cassandra
      OPTIONS (
       table "transactions",
       keyspace "rtfap",
       cluster "Test Cluster",
       pushdown "true"
      )""")

    //hourlyaggregates_bycc
    val rollup4= sqlContext.sql("select cc_no, int(concat(year,month,day,hour)) as hour, sum(amount) as total_amount, min(amount) as min_amount, max(amount) as max_amount, count(*) as total_count from temp_transactions group by cc_no, int(concat(year,month,day,hour))")
    rollup4.write.format("org.apache.spark.sql.cassandra")
      .mode(SaveMode.Overwrite)
      .options(Map("keyspace" -> "rtfap", "table" -> "hourlyaggregates_bycc"))
      .save()

    // dailyaggregations_bycc
    val rollup3= sqlContext.sql("select cc_no, int(concat(year,month,day)) as day, sum(amount) as total_amount, min(amount) as min_amount, max(amount) as max_amount, count(*) as total_count from temp_transactions group by cc_no, int(concat(year,month,day))")
    rollup3.write.format("org.apache.spark.sql.cassandra")
      .mode(SaveMode.Overwrite)
      .options(Map("keyspace" -> "rtfap", "table" -> "dailyaggregates_bycc"))
      .save()

    // monthlyaggregations_bycc
    val rollup5= sqlContext.sql("select cc_no, int(concat(year,month)) as month, sum(amount) as total_amount, min(amount) as min_amount, max(amount) as max_amount, count(*) as total_count from temp_transactions group by cc_no, int(concat(year,month))")
    rollup5.write.format("org.apache.spark.sql.cassandra")
      .mode(SaveMode.Overwrite)
      .options(Map("keyspace" -> "rtfap", "table" -> "monthlyaggregates_bycc"))
      .save()

    // yearlyaggregations_bycc
    val rollup6= sqlContext.sql("select cc_no, int(year) as year, sum(amount) as total_amount, min(amount) as min_amount, max(amount) as max_amount, count(*) as total_count from temp_transactions group by cc_no, int(year)")
    rollup6.write.format("org.apache.spark.sql.cassandra")
      .mode(SaveMode.Overwrite)
      .options(Map("keyspace" -> "rtfap", "table" -> "yearlyaggregates_bycc"))
      .save()


    println("Completed RollUps By CC...")
  }
}