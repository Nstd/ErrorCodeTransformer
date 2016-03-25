
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ECodeTransformer {

	private static final String CONFIG = "config.properties";
	private static Map<String, String> config = new HashMap<>();
	
	
	public static void main(String[] args) throws Exception {
		if (!initProperties()) {
			System.out.println("读取配置文件失败!");
			System.exit(1);
		}
		
		String sourceStr = config.get("source").toString();
		String destStr = config.get("dest").toString();
		JSONObject jSource = null;
		JSONObject jDest = null;

		if (isEmpty(sourceStr)) {
			System.out.println("未能找到config.source");
			System.exit(1);
		} else {
			sourceStr = readFile(sourceStr);
			if (isEmpty(sourceStr)) {
				System.out.println("source文件为空");
				System.exit(1);
			}
		}
		
		if (isEmpty(destStr)) {
			System.out.println("未能找到config.dest");
			System.exit(1);
		}
		
		String[] destArray = destStr.split(",");
		boolean allSuccess = true;
		for(int i = 0; i < destArray.length; i++) {
			String destName = destArray[i];
			File dFile = new File(destName);
			File dParentFile = dFile.getParentFile();
			System.out.println("处理:" + destName);
			if(!dFile.exists()) {
				// 如果不存在直接写入dest
				if(!dParentFile.exists()) {
					dParentFile.mkdirs();
				}
				if(!writeFile(destName, sourceStr)) {
					allSuccess = false;;
				}
			} else {
				// 进行diff
				String destJson = readFile(destName);
				
				jSource = new JSONObject(sourceStr);
				jDest = new JSONObject(destJson);
				
				diffJson(jSource, jDest, 1);
				if(!writeFile(destName, formatJson(jSource.toString()))) {
					allSuccess = false;
				}
			}
			System.out.println("");
		}
		
		System.out.println("转换结束");
		
		System.exit(allSuccess ? 0 : 1);
		
	}
	
	public static void diffJson(JSONObject source, JSONObject dest, int level) {
		Iterator<String> dKeys = dest.keys();
		while(dKeys.hasNext()) {
			String key = dKeys.next();
//			System.out.println("level: " + level + " key:" + key);
			Object value = null;
			try {
				value = source.get(key);
			} catch(Exception e) {
				
			}
			
			if(level == 1 || level == 2) {
//				if(value == null) {
//					source.put(key, value);
//				} else {
				if(value != null) {
					diffJson(source.getJSONObject(key), dest.getJSONObject(key), level+1);
				}
			} else {
				if(value == null) {
					System.out.println("put (" + key + ","+ dest.get(key) + ")");
					source.put(key, dest.get(key));
				} else {
					continue;
				}
			}
		}
	}
	
	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}
	
	public static boolean initProperties() {
		Properties prop = new Properties();
		InputStream in = null;
		try {
			in = new BufferedInputStream (new FileInputStream(CONFIG)); //new BufferedInputStream(ECodeTransformer.class.getResourceAsStream(CONFIG));
			prop.load(in);
			Iterator<String> it = prop.stringPropertyNames().iterator();
			while(it.hasNext()) {
				String key = it.next();
				config.put(key, prop.getProperty(key));
			}
			
			return true;
		} catch (IOException e) {
			System.out.println("未能读取config.properties");
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return false;
	}
	
	public static String readFile(String path) {
		StringBuffer sb = new StringBuffer();
		BufferedReader br = null;
		try {
			File file=new File(path);
			if (!file.exists() || file.isDirectory())
			    throw new FileNotFoundException();
			br = new BufferedReader(new FileReader(file));
			String temp = null;
			temp=br.readLine();
			while(temp != null) {
			    sb.append(temp+" ");
			    temp = br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
        return sb.toString();
    }
	
	public static boolean writeFile(String fileName, String data) {
		try {
			FileWriter writer = new FileWriter(fileName);
			writer.write(data);
			writer.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static String formatJson(String json) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(json);
		String prettyJsonStr = gson.toJson(je);
		return prettyJsonStr;
	}
}
