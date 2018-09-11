package com.hbase.api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import com.opencsv.CSVWriter;

public class App{
	static TableName table1 = TableName.valueOf("test_data");
	static String family1 = "segments";
	static String rowKey = "1467";
	static String propFile = "/home/srgmsbi1417/hbase-site.xml";
	static String outFile = "/home/srgmsbi1417/output.csv";
	
	public static void main(String[] args) throws IOException, ServiceException {
		try 
		{
			Configuration config = HBaseConfiguration.create();
			config.addResource(new Path(propFile));
			HBaseAdmin.checkHBaseAvailable(config);
			Connection connection = ConnectionFactory.createConnection(config);
			Table table = connection.getTable(table1);
			Get q= new Get(Bytes.toBytes(rowKey));
			q.setMaxVersions(500);
			Result row= table.get(q);
			NavigableMap<byte[],NavigableMap<byte[],NavigableMap<Long,byte[]>>> allVersions=row.getMap();
			Map<String,Map<String,String>> response;
			String prevValue=null;
			String currValue=null;
			int count=0;
			File file = new File(outFile);
			FileWriter outputfile = new FileWriter(file); 
			CSVWriter writer = new CSVWriter(outputfile);
			String[] header = { "RowKey", "Version", "Path", "OldValue", "NewValue" }; 
		    writer.writeNext(header);
			for(Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> l1: allVersions.entrySet()) {
				for(Entry<byte[], NavigableMap<Long, byte[]>> l2: l1.getValue().entrySet()) {
					for(Entry<Long, byte[]> l3: l2.getValue().descendingMap().entrySet()) {
						currValue=new String(l3.getValue());
						if(prevValue==null) {
							writer.writeNext(new String[]{rowKey, Integer.toString(count), "", "" , ""});
						}
						else {
							response = getDiff(prevValue, currValue);
							for(String key :response.keySet()){
								writer.writeNext(new String[]{rowKey, Integer.toString(count), key, response.get(key).get("old"), response.get(key).get("new")});
							}
						}
						prevValue=currValue;
						count++;
					}
				}
			}
			writer.close();
			System.out.println("\n\n\nProcessing Completed." );
		}
		catch (IOException e) { 
	        e.printStackTrace(); 
	    } 
	}
	
	private static Map<String,Map<String,String>> getDiff(String oldJson, String newJson) throws JsonProcessingException, IOException {
		ObjectMapper jackson = new ObjectMapper();
		Map<String,Map<String,String>> response = new HashMap<String, Map<String,String>>();
		JsonNode oldJsonNode = jackson.readTree(oldJson); 
		JsonNode newJsonNode = jackson.readTree(newJson);
		JsonNode patchNode = JsonDiff.asJson(oldJsonNode, newJsonNode); 
		if (patchNode.isArray()) {
		    for (final JsonNode objNode : patchNode) {
				JsonNode theOldNode = null;
		        JsonNode theNewNode = null;
		        String path = objNode.get("path").textValue();
		        Map<String,String> values = new HashMap<String, String>();
	        	theOldNode = oldJsonNode.at(path);
	        	theNewNode = newJsonNode.at(path);
		        if(theOldNode != null ) {
		        	values.put("old", theOldNode.asText());
		        }
		        if(theNewNode != null ) {
		        	values.put("new", theNewNode.asText());
		        }
		        response.put(path, values);
		    }
		}
		return response;
	}
}