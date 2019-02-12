import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonToEntity {

    private JsonToEntity(){}

    private static final String METHOD_PRE = "set";
    private static final String INT_STR = "int";
    private static final String INTEGER_STR = "java.lang.Integer";
    private static final String LONG_STR_1 = "long";
    private static final String LONG_STR_2 = "java.lang.Long";
    private static final String DOUBLE_STR_1 = "double";
    private static final String DOUBLE_STR_2 = "java.lang.Double";
    private static final String FLOAT_STR_1 = "float";
    private static final String FLOAT_STR_2 = "java.lang.Float";
    private static final String DATE_STR = "java.util.Date";
    private static final String BOOLEAN_STR_1 = "boolean";
    private static final String BOOLEAN_STR_2 = "java.lang.Boolean";

    private static List<Map<String,String>> objProps = new ArrayList<>();

    public static <T> List<T> convert2Obj(Class<T> clazz,String jsonStr,int initLevel) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, ParseException {
        List<Map<String,String>> objs;
        List<String> fields;
        objs = convert2KVparis(jsonStr,initLevel);
        fields = doesObjectContainField(clazz);
        List<T> objList = new ArrayList<>();
        for(Map<String,String> map:objs) {
            T t = clazz.getDeclaredConstructor().newInstance();
            for (String fieldName : map.keySet())
                if (fields.contains(fieldName)) {
                    String methodName = METHOD_PRE + captureName(fieldName);
                    Class<?> fieldType = clazz.getDeclaredField(fieldName).getType();
                    Method m = clazz.getDeclaredMethod(methodName, fieldType);
                    Object finalValue;
                    String fieldType_Name = fieldType.getName();
                    String field_Value = map.get(fieldName);
                    if(field_Value==null||field_Value.length()==0) continue;
                    if (fieldType_Name.equals(INT_STR) || fieldType_Name.equals(INTEGER_STR)) {
                        finalValue = Integer.parseInt(field_Value);
                    } else if (fieldType_Name.equals(BOOLEAN_STR_1) || fieldType_Name.equals(BOOLEAN_STR_2)) {
                        finalValue = Boolean.parseBoolean(field_Value);
                    } else if (fieldType_Name.equals(LONG_STR_1) || fieldType_Name.equals(LONG_STR_2)) {
                        finalValue = Long.parseLong(field_Value);
                    } else if (fieldType_Name.equals(DOUBLE_STR_1) || fieldType_Name.equals(DOUBLE_STR_2)) {
                        finalValue = Double.parseDouble(field_Value);
                    } else if (fieldType_Name.equals(FLOAT_STR_1) || fieldType_Name.equals(FLOAT_STR_2)) {
                        finalValue = Float.parseFloat(field_Value);
                    } else if (fieldType_Name.equals(DATE_STR)) {
                        //TODO need to add more Date String pattern!
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        finalValue = formatter.parse(field_Value);
                    } else {
                        finalValue = field_Value;
                    }
                    m.invoke(t, finalValue);
                }
            objList.add(t);
        }
        return objList;
    }

    /*
     * @author Zhenzhong Mao
     * @param json file string
     * @return a list of key value pairs represent json properties
     */
    public static List<Map<String,String>> convert2KVparis(String jsonStr, int level)  {
        Map<String,String> newMap = new HashMap<>();
        json2ObjHelper(JSONObject.parse(jsonStr),newMap,level);
        return objProps;
    }

    private static void json2ObjHelper(Object obj, Map<String,String> valuesMap, int level)  {
        Set<String> objstrs = ((JSONObject) obj).keySet();
        for(String str : objstrs) {
            if(((JSONObject) obj).get(str) instanceof JSONObject) {
                level++;
                json2ObjHelper(((JSONObject) obj).get(str),valuesMap,level);
                level--;
            }else if(((JSONObject) obj).get(str) instanceof JSONArray){
                JSONArray arr = ((JSONObject) obj).getJSONArray(str);
                for(int i=0;i<arr.size();i++) {
                    level++;
                    JSONObject tmpobj  = arr.getJSONObject(i);
                    json2ObjHelper(tmpobj,valuesMap,level);
                    level--;
                }
            }
            else {
                String tmp = ((JSONObject) obj).get(str).toString();
                valuesMap.put(str, tmp);
            }
        }
        if(level==1) {
            Map<String,String> tmpMap = new HashMap<>(valuesMap);
            objProps.add(tmpMap);
            valuesMap = new HashMap<>();
        }

    }

    private static <T> List<String> doesObjectContainField(Class<T> c) {
        List<String> fieldNames = new ArrayList<>();
        for(Field field :c.getDeclaredFields()) {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    private static String captureName(String str) {
        char[] cs=str.toCharArray();
        cs[0]-=32;
        return String.valueOf(cs);
    }
}

