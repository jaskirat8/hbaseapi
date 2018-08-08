package com.hbase.api;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.protobuf.ServiceException;

public class App{
	static TableName table1 = TableName.valueOf("test_data");
	static String family1 = "segments";
	static String rowKey = "1467";
	static String propFile = "/home/srgmsbi1417/hbase-site.xml";
	
	public static void main(String[] args) throws IOException, ServiceException {
		Configuration config = HBaseConfiguration.create();
		config.addResource(new Path(propFile));
		HBaseAdmin.checkHBaseAvailable(config);
		Connection connection = ConnectionFactory.createConnection(config);
		Table table = connection.getTable(table1);
		Get q= new Get(Bytes.toBytes(rowKey));
		q.setMaxVersions(500);
		Result row= table.get(q);
		NavigableMap<byte[],NavigableMap<byte[],NavigableMap<Long,byte[]>>> allVersions=row.getMap();
		String response="";
		String prevValue=null;
		String currValue=null;
		int count=0;
		for(Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> l1: allVersions.entrySet()) {
			for(Entry<byte[], NavigableMap<Long, byte[]>> l2: l1.getValue().entrySet()) {
				for(Entry<Long, byte[]> l3: l2.getValue().descendingMap().entrySet()) {
					currValue=new String(l3.getValue());
					if(prevValue==null) {
						response += Integer.toString(count) + "," + currValue;
					}
					else {
						response += ";_;" + Integer.toString(count) + "," + getDiff(prevValue, currValue);
					}
					prevValue=currValue;
					count++;
				}
			}
		}
		
		System.out.println(response + "\n\n\nProcessing Completed." );
	}
	
	private static String getDiff(String oldJson, String newJson) throws JsonProcessingException, IOException {
		ObjectMapper jackson = new ObjectMapper(); 
		JsonNode oldJsonNode = jackson.readTree(oldJson); 
		JsonNode newJsonNode = jackson.readTree(newJson);
		JsonNode patchNode = JsonDiff.asJson(oldJsonNode, newJsonNode); 
//		if (patchNode.isArray()) {
//		    for (final JsonNode objNode : patchNode) {
//		        System.out.println(objNode.get("path"));
//		    }
//		}
		return patchNode.toString();
	}
}