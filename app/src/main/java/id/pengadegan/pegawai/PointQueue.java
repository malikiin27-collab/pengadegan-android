package id.pengadegan.pegawai;
import android.content.*;import android.database.*;import android.database.sqlite.*;import org.json.*;
final class PointQueue extends SQLiteOpenHelper {
 PointQueue(Context c){super(c,"tracking_queue.db",null,1);}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE points(id TEXT PRIMARY KEY,session_id INTEGER NOT NULL,payload TEXT NOT NULL)");}
 public void onUpgrade(SQLiteDatabase d,int a,int b){}
 synchronized void owner(String value){SQLiteDatabase d=getWritableDatabase();d.execSQL("CREATE TABLE IF NOT EXISTS owner(value TEXT)");try(Cursor c=d.rawQuery("SELECT value FROM owner LIMIT 1",null)){if(c.moveToFirst()&&value.equals(c.getString(0)))return;}d.delete("points",null,null);d.delete("owner",null,null);ContentValues v=new ContentValues();v.put("value",value);d.insertOrThrow("owner",null,v);}
 synchronized boolean add(JSONObject p)throws JSONException{SQLiteDatabase d=getWritableDatabase();try(Cursor c=d.rawQuery("SELECT COUNT(*) FROM points",null)){if(c.moveToFirst()&&c.getInt(0)>=10000)return false;}ContentValues v=new ContentValues();v.put("id",p.getString("id"));v.put("session_id",p.getLong("session_id"));v.put("payload",p.toString());d.insertOrThrow("points",null,v);return true;}
 synchronized JSONArray batch()throws JSONException{JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM points ORDER BY rowid LIMIT 100",null)){while(c.moveToNext())a.put(new JSONObject(c.getString(0)));}return a;}
 synchronized void ack(JSONArray ids){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{for(int i=0;i<ids.length();i++)d.delete("points","id=?",new String[]{ids.optString(i)});d.setTransactionSuccessful();}finally{d.endTransaction();}}
 synchronized int size(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM points",null)){return c.moveToFirst()?c.getInt(0):0;}}
}
