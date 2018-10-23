// https://searchcode.com/api/result/126638312/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package db;

import articlybase.*;
import com.mongodb.*;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.types.ObjectId;

/**
 *
 * @author pavel
 */
public class DataMapper {

    public static final int CREATEDATE = 0;
    public static final int VIEWS = 1;
    public static final int BUYERS = 2;
    public static final int COST = 3;
    public static final int ASCENDING = 1;
    public static final int DESCENDING = -1;
    private static final byte[] salt = {'a', 'r', 't', 'i', 'c', 'l', 'y'};
    private String db_addr;
    private int db_port;
    private static Mongo mongo;
    private static DB db;
    public static DataMapper instance = null;
    // collections
    DBCollection articles_c;
    DBCollection authors_c;
    DBCollection notices_c;
    DBCollection readers_c;
    DBCollection subscriptions_c;
    DBCollection transactions_c;

    private DataMapper() {
    }

    public static DataMapper getInstance() {
        return instance;
    }

    public static Field[] concat(Field[] o1, Field[] o2) {
        Field[] ret = new Field[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static String passwordHash(String password) {
        MessageDigest md = null;
        String password_hash = "";
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
        }
        md.reset();
        md.update(salt);
        try {
            password_hash = byteArrayToHexString(md.digest(password.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger("ArticlyLog").log(Level.SEVERE, null, ex);
        }
        return password_hash;
    }

    public Object convert(DBObject obj, String cls) throws Exception {
        System.out.println("convert: " + obj);
        Object converted = null;
        Class convert_cls;
        convert_cls = Class.forName(Article.class.getPackage().getName() + "." + cls);
        converted = convert_cls.newInstance();

        Field[] fields = concat(convert_cls.getDeclaredFields(),
                convert_cls.getSuperclass().getDeclaredFields());

        for (Field field : fields) {
            try {
                int m = field.getModifiers();
                if (((m & Modifier.FINAL) == 0) && ((m & Modifier.PUBLIC) != 0)) {
                    field.set(converted, (obj.get(field.getName())));
                }
            } catch (IllegalArgumentException ex) {
            } catch (IllegalAccessException ex) {
            }
        }
        return converted;
    }

    public static boolean init(String addr, int port) {
        return init(addr, port, "test");
    }

    public static boolean init(String addr, int port, String db_name) {
        if (instance == null) {
            instance = new DataMapper();
            instance.db_addr = addr;
            instance.db_port = port;
            //setup connection
            try {
                mongo = new Mongo(instance.db_addr, instance.db_port);
                db = mongo.getDB(db_name);
                //db.setWriteConcern(WriteConcern.SAFE);
                instance.articles_c = db.getCollection("articles");
                instance.authors_c = db.getCollection("authors");
                instance.notices_c = db.getCollection("notices");
                instance.readers_c = db.getCollection("readers");
                instance.subscriptions_c = db.getCollection("subscriptions");
                instance.transactions_c = db.getCollection("transactions");

                //resume transactions
                instance.resume();

            } catch (UnknownHostException ex) {
                Logger.getLogger("ArticlyLog").log(Level.SEVERE, "UnknownHostException: {0}", ex.getMessage());
                return false;
            } catch (MongoException ex) {
                Logger.getLogger("ArticlyLog").log(Level.SEVERE, "UnknownHostException: {0}", ex.getMessage());
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private void resume() {
        BasicDBObject id_obj = new BasicDBObject();
        BasicDBList or_list = new BasicDBList();
        BasicDBObject pending = new BasicDBObject();
        BasicDBObject commited = new BasicDBObject();
        pending.put("state", Transaction.PENDING);
        commited.put("state", Transaction.COMMITED);
        or_list.add(pending);
        or_list.add(commited);
        id_obj.put("$or", or_list);
        DBCursor cur = subscriptions_c.find(id_obj);
        while (cur.hasNext()) {
            try {
                Transaction tr = (Transaction) convert(cur.next(), "Transaction");
                processTransaction(tr, cur.next());
            } catch (Exception ex) {
                System.out.println("Resume error: " + ex.getMessage());
            }
        }
    }

    public void store(Object obj) {
        System.out.println("Store");
        System.out.println("addr: " + this.db_addr);
        System.out.println("port: " + this.db_port);
        System.out.println(obj.getClass().getSimpleName());

        Field[] fields = concat(obj.getClass().getDeclaredFields(),
                obj.getClass().getSuperclass().getDeclaredFields());
        BasicDBObject id_obj = new BasicDBObject();
        BasicDBObject store_obj = new BasicDBObject();

        for (Field field : fields) {
            try {
                int m = field.getModifiers();
                //System.out.println(Modifier.FINAL & m);
                if (((m & Modifier.FINAL) == 0) && ((m & Modifier.PUBLIC) != 0)) {
                    // not final fields
                    //System.out.println(field.getName() + ": " + field.get(obj));
                    store_obj.put(field.getName(), field.get(obj));
                }
            } catch (IllegalArgumentException ex) {
            } catch (IllegalAccessException ex) {
            }
        }
        System.out.println("store: " + store_obj);
        String clsname = obj.getClass().getSimpleName();
        if ("Article".equals(clsname)) {
            if (((Article) obj).id == null) {
                ((Article) obj).id = new ObjectId().toString();
                store_obj.put("id", ((Article) obj).id);
            }
            id_obj.put("id", ((Article) obj).id);
            articles_c.update(id_obj, store_obj, true, false);
            //((Article) obj).id = ((ObjectId) store_obj.get("_id")).toString();
        } else if ("Author".equals(clsname)) {
            id_obj.put("name", ((Author) obj).name);
            authors_c.update(id_obj, store_obj, true, false);
        } else if ("Notice".equals(clsname)) {
            if (((Notice) obj).id == null) {
                ((Notice) obj).id = new ObjectId().toString();
                store_obj.put("id", ((Notice) obj).id);
            }
            id_obj.put("id", ((Notice) obj).id);
            notices_c.update(id_obj, store_obj, true, false);
            //((Notice) obj).id = ((ObjectId) store_obj.get("_id")).toString();
        } else if ("Reader".equals(clsname)) {
            id_obj.put("name", ((Reader) obj).name);
            readers_c.update(id_obj, store_obj, true, false);
        } else if ("Subscription".equals(clsname)) {
            if (((Subscription) obj).id == null) {
                ((Subscription) obj).id = new ObjectId().toString();
                store_obj.put("id", ((Subscription) obj).id);
            }
            id_obj.put("id", ((Subscription) obj).id);
            subscriptions_c.update(id_obj, store_obj, true, false);
            //((Subscription) obj).id = ((ObjectId) store_obj.get("_id")).toString();
        } else if ("Transaction".equals(clsname)) {
            if (((Transaction) obj).id == null) {
                ((Transaction) obj).id = new ObjectId().toString();
                store_obj.put("id", ((Transaction) obj).id);
                transactions_c.save(store_obj);
            }
            processTransaction((Transaction) obj, store_obj);
            //id_obj.put("id", ((Transaction) obj).id);
            //transactions_c.update(id_obj, store_obj, true, false);
            //((Notice) obj).id = ((ObjectId) store_obj.get("_id")).toString();
        }
    }

    public void remove(Object obj) {
        System.out.println("Remove");
        System.out.println("addr: " + this.db_addr);
        System.out.println("port: " + this.db_port);

        BasicDBObject id_obj = new BasicDBObject();
        String clsname = obj.getClass().getSimpleName();
        if ("Subscription".equals(clsname)) {
            id_obj.put("id", ((Subscription) obj).id);
            subscriptions_c.remove(id_obj);
        } else if ("Reader".equals(clsname)) {
            id_obj.put("name", ((Reader) obj).name);
            readers_c.remove(id_obj);
        } else if ("Author".equals(clsname)) {
            id_obj.put("name", ((Author) obj).name);
            authors_c.remove(id_obj);
        } else if ("Notice".equals(clsname)) {
            id_obj.put("id", ((Notice) obj).id);
            notices_c.remove(id_obj);
        } else if ("Article".equals(clsname)) {
            id_obj.put("id", ((Article) obj).id);
            articles_c.remove(id_obj);
        } else if ("Transaction".equals(clsname)) {
            id_obj.put("id", ((Transaction) obj).id);
            transactions_c.remove(id_obj);
        }
    }

    public Article loadArticle(String article_id) throws Exception {
        System.out.println("Load article: " + article_id);
        Article ret;
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("id", article_id);
        ret = (Article) convert(articles_c.findOne(id_obj), "Article");
        return ret;
    }

    public List<Article> loadArticles() throws Exception {
        System.out.println("Load articles");
        List<Article> result = new ArrayList<Article>();
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("removed", false);
        DBCursor cur = articles_c.find(id_obj);
        while (cur.hasNext()) {
            Article a = (Article) convert(cur.next(), "Article");
            result.add(a);
        }
        return result;
    }

    public List<Article> loadArticles(final int field, final int order) throws Exception {
        int index = 0;
        int count = (int) articles_c.count();
        return this.loadArticles(field, order, index, count);
    }

    public List<Article> loadArticles(final int field, final int order, int index, int count) throws Exception {
        System.out.println("Load articles");
        List<Article> result = new ArrayList<Article>();
        BasicDBObject sort_obj = new BasicDBObject();
        switch (field) {
            case DataMapper.CREATEDATE:
                sort_obj.put("created", order);
                break;
            case DataMapper.VIEWS:
                sort_obj.put("readed", order);
                break;
            case DataMapper.BUYERS:
                sort_obj.put("buyers", order);
                break;
            case DataMapper.COST:
                sort_obj.put("cost", order);
                break;
            default:
                break;
        }
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("removed", false);
        DBCursor cur = articles_c.find(id_obj).sort(sort_obj).skip(index).limit(count);
        while (cur.hasNext()) {
            Article a = (Article) convert(cur.next(), "Article");
            result.add(a);
        }
        return result;
    }

    public Author loadAuthor(String author_name) throws Exception {
        System.out.println("Load author: " + author_name);
        Author ret;
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("name", author_name);
        ret = (Author) convert(authors_c.findOne(id_obj), "Author");
        return ret;
    }

    public List<Author> loadAuthors() throws Exception {
        System.out.println("Load authors");
        List<Author> result = new ArrayList<Author>();
        DBCursor cur = authors_c.find();
        while (cur.hasNext()) {
            Author a = (Author) convert(cur.next(), "Author");
            result.add(a);
        }
        return result;
    }

    public Notice loadNotice(String notice_id) throws Exception {
        System.out.println("Load notice: " + notice_id);
        Notice ret;
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("id", notice_id);
        ret = (Notice) convert(notices_c.findOne(id_obj), "Notice");
        return ret;
    }

    public List<Notice> loadNotices() throws Exception {
        System.out.println("Load notices");
        List<Notice> result = new ArrayList<Notice>();
        DBCursor cur = notices_c.find();
        while (cur.hasNext()) {
            Notice n = (Notice) convert(cur.next(), "Notice");
            result.add(n);
        }
        return result;
    }

    public List<Notice> loadNotices(String reader_name) throws Exception {
        System.out.println("Load notices of " + reader_name);
        List<Notice> result = new ArrayList<Notice>();
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("reader_name", reader_name);
        DBCursor cur = notices_c.find(id_obj);
        while (cur.hasNext()) {
            Notice n = (Notice) convert(cur.next(), "Notice");
            result.add(n);
        }
        return result;
    }

    public Reader loadReader(String reader_name) throws Exception {
        System.out.println("Load reader: " + reader_name);
        Reader ret;
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("name", reader_name);
        ret = (Reader) convert(readers_c.findOne(id_obj), "Reader");
        return ret;
    }

    public List<Reader> loadReaders() throws Exception {
        System.out.println("Load readers");
        List<Reader> result = new ArrayList<Reader>();
        DBCursor cur = readers_c.find();
        while (cur.hasNext()) {
            Reader r = (Reader) convert(cur.next(), "Reader");
            result.add(r);
        }
        return result;
    }

    public Subscription loadSubscription(String subscription_id) throws Exception {
        System.out.println("Load subscription: " + subscription_id);
        Subscription ret;
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("id", subscription_id);
        ret = (Subscription) convert(subscriptions_c.findOne(id_obj), "Subscription");
        return ret;
    }

    public List<Subscription> loadSubscriptions() throws Exception {
        System.out.println("Load subscriptions");
        List<Subscription> result = new ArrayList<Subscription>();
        DBCursor cur = subscriptions_c.find();
        while (cur.hasNext()) {
            Subscription s = (Subscription) convert(cur.next(), "Subscription");
            result.add(s);
        }
        return result;
    }

    public List<Subscription> loadSubscriptions(String article_id) throws Exception {
        System.out.println("Load subscriptions of " + article_id);
        Article article = this.loadArticle(article_id);
        List<Subscription> result = new ArrayList<Subscription>();
        //find subscriptions with article author or article category
        BasicDBObject id_obj = new BasicDBObject();
        BasicDBList or_list = new BasicDBList();
        BasicDBObject author_sub = new BasicDBObject();
        BasicDBObject category_sub = new BasicDBObject();
        author_sub.put("type", Subscription.AUTHOR);
        author_sub.put("targets", article.author_name);
        category_sub.put("type", Subscription.CATEGORY);
        category_sub.put("targets", article.category);
        or_list.add(author_sub);
        or_list.add(category_sub);
        id_obj.put("$or", or_list);

        //group by reader_name
        BasicDBObject key = new BasicDBObject();
        key.put("reader_name", true);
        BasicDBObject cond = new BasicDBObject();
        BasicDBObject initial = new BasicDBObject();
        String reduce = "function(o,p) {p.id = o.id; p.type = o.type; p.targets = o.targets; p.created = o.created;}";

        BasicDBList ret = (BasicDBList) subscriptions_c.group(key, cond, initial, reduce);
        for (Object obj : ret) {
            //hack: after grouping int become double?
            //((DBObject)obj).put("type", (int) Double.parseDouble(((DBObject)obj).get("type").toString()));
            Subscription s = (Subscription) convert((DBObject) obj, "Subscription");
            result.add(s);
        }
        return result;
    }

    public List<Transaction> loadTransactions() throws Exception {
        System.out.println("Load transactions");
        List<Transaction> result = new ArrayList<Transaction>();
        DBCursor cur = subscriptions_c.find();
        while (cur.hasNext()) {
            Transaction tr = (Transaction) convert(cur.next(), "Transaction");
            result.add(tr);
        }
        return result;
    }

    public boolean exist(Object obj) {
        String clsname = obj.getClass().getSimpleName();
        if ("Author".equals(clsname) || "Reader".equals(clsname)) {
            BasicDBObject id_obj = new BasicDBObject();
            id_obj.put("name", ((Reader) obj).name);
            if (authors_c.find(id_obj).count() > 0 || readers_c.find(id_obj).count() > 0) {
                return true;
            }
        }
        return false;
    }

    public List<String> getCategories() {
        System.out.println("Get articles categories");
        List<String> result = new ArrayList<String>();
        BasicDBObject key = new BasicDBObject();
        key.put("category", true);
        BasicDBObject cond = new BasicDBObject();
        BasicDBObject initial = new BasicDBObject();
        String reduce = "function(o,p) {}";
        BasicDBList ret = (BasicDBList) articles_c.group(key, cond, initial, reduce);
        for (Object obj : ret) {
            result.add((String) ((DBObject) obj).get("category"));
        }
        return result;
    }

    public Reader login(String name, String password) throws Exception {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
        }
        md.reset();
        md.update(salt);
        String password_hash = "";
        password_hash = byteArrayToHexString(md.digest(password.getBytes("UTF-8")));

        //Reader ret = null;
        DBObject ret_obj;
        // try login as reader
        System.out.println("HASH: " + password_hash);
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("name", name);
        id_obj.put("password", password_hash);
        ret_obj = readers_c.findOne(id_obj);
        if (ret_obj == null) {
            // try login as author
            ret_obj = authors_c.findOne(id_obj);
            if (ret_obj == null) {
                throw new Exception("Wrong login or password: " + name + "; " + password + ";");
            } else {
                return (Author) convert(ret_obj, "Author");
            }
        } else {
            return (Reader) convert(ret_obj, "Reader");
        }
    }

    public Reader login(String cookie) throws Exception {
        DBObject ret_obj;
        // try login as reader
        System.out.println("login cookie: " + cookie);
        BasicDBObject id_obj = new BasicDBObject();
        id_obj.put("cookie", cookie);
        ret_obj = readers_c.findOne(id_obj);
        if (ret_obj == null) {
            // try login as author
            ret_obj = authors_c.findOne(id_obj);
            if (ret_obj == null) {
                throw new Exception("Wrong cookie: " + cookie + ";");
            } else {
                return (Author) convert(ret_obj, "Author");
            }
        } else {
            return (Reader) convert(ret_obj, "Reader");
        }
    }

    private void processTransaction(Transaction transaction, DBObject store_obj) {
        BasicDBObject id_obj = new BasicDBObject();
        BasicDBObject pending_obj = new BasicDBObject();
        pending_obj.put("pending_tansactions", transaction.id);
        id_obj.put("id", transaction.id);

        //make two phase commit
        switch (transaction.state) {
            case Transaction.INIT:
                //1 step
                transaction.state = Transaction.PENDING;
                store_obj.put("state", transaction.state);
                //transactions_c.update(id_obj, store_obj, true, false);
                transactions_c.update(id_obj, store_obj);

            case Transaction.PENDING:
                //2 step
                    /*
                 * foo:PRIMARY> db.accounts.update({name: t.source,
                 * pendingTransactions: {$ne: t._id}}, {$inc: {balance:
                 * -t.value}, $push: {pendingTransactions: t._id}}) foo:PRIMARY>
                 * db.accounts.update({name: t.destination, pendingTransactions:
                 * {$ne: t._id}}, {$inc: {balance: t.value}, $push:
                 * {pendingTransactions: t._id}})
                 */

                BasicDBObject source_id_obj = new BasicDBObject();
                BasicDBObject source_store_obj = new BasicDBObject();
                BasicDBObject destination_id_obj = new BasicDBObject();
                BasicDBObject destination_store_obj = new BasicDBObject();

                source_id_obj.put("name", transaction.source_id);
                BasicDBObject source_ne_obj = new BasicDBObject();
                source_ne_obj.put("$ne", transaction.id);
                source_id_obj.put("pending_tansactions", source_ne_obj);

                BasicDBObject source_inc_obj = new BasicDBObject();
                source_inc_obj.put("account", -transaction.value);
                source_store_obj.put("$inc", source_inc_obj);
                BasicDBObject push_list = new BasicDBObject();
                push_list.put("pending_tansactions", transaction.id);
                push_list.put("articles", transaction.article_id);
                source_store_obj.put("$push", push_list);

                destination_id_obj.put("name", transaction.destination_id);
                BasicDBObject dest_ne_obj = new BasicDBObject();
                dest_ne_obj.put("$ne", transaction.id);
                destination_id_obj.put("pending_tansactions", dest_ne_obj);

                BasicDBObject dest_inc_obj = new BasicDBObject();
                dest_inc_obj.put("account", transaction.value);
                destination_store_obj.put("$inc", dest_inc_obj);
                BasicDBObject pending_obj2 = new BasicDBObject();
                pending_obj2.put("pending_tansactions", transaction.id);
                destination_store_obj.put("$push", pending_obj2);

                int source_n = 0;
                authors_c.update(source_id_obj, source_store_obj);
                source_n += (Integer) db.getLastError().get("n");
                readers_c.update(source_id_obj, source_store_obj);
                source_n += (Integer) db.getLastError().get("n");
                if (source_n != 1) {
                    System.out.println("Transaction source error in step 2 n = " + source_n);
                    //return;
                }

                int dest_n = 0;
                authors_c.update(destination_id_obj, destination_store_obj);
                dest_n += (Integer) db.getLastError().get("n");
                readers_c.update(destination_id_obj, destination_store_obj);
                dest_n += (Integer) db.getLastError().get("n");
                if (dest_n != 1) {
                    System.out.println("Transaction destination error in step 2");
                    //return;
                }
                
                transaction.source.account -= transaction.value;
                transaction.source.articles.add(transaction.article_id);
                //+1 to article readed
                BasicDBObject article_id_obj = new BasicDBObject();
                article_id_obj.put("id", transaction.article_id);
                BasicDBObject article_store_obj = new BasicDBObject();
                BasicDBObject article_inc_obj = new BasicDBObject();
                article_inc_obj.put("readed", 1);
                article_store_obj.put("$inc", article_inc_obj);

                //3 step
                transaction.state = Transaction.COMMITED;
                store_obj.put("state", transaction.state);
                //System.out.println("step3: " + store_obj);
                transactions_c.update(id_obj, store_obj);

            case Transaction.COMMITED:
                //4 step
                    /*
                 * foo:PRIMARY> db.accounts.update({name: t.source}, {$pull:
                 * {pendingTransactions: ObjectId("4d7bc7a8b8a04f5126961522")}})
                 * foo:PRIMARY> db.accounts.update({name: t.destination},
                 * {$pull: {pendingTransactions:
                 * ObjectId("4d7bc7a8b8a04f5126961522")}})
                 */

                BasicDBObject source_id_obj2 = new BasicDBObject();
                BasicDBObject destination_id_obj2 = new BasicDBObject();
                source_id_obj2.put("name", transaction.source_id);
                destination_id_obj2.put("name", transaction.destination_id);

                BasicDBObject source_store_obj2 = new BasicDBObject();
                BasicDBObject destination_store_obj2 = new BasicDBObject();

                source_store_obj2.put("$pull", pending_obj);
                destination_store_obj2.put("$pull", pending_obj);

                source_n = 0;
                authors_c.update(source_id_obj2, source_store_obj2);
                source_n += (Integer) db.getLastError().get("n");
                readers_c.update(source_id_obj2, source_store_obj2);
                source_n += (Integer) db.getLastError().get("n");
                if (source_n != 1) {
                    System.out.println("Transaction source error in step 4");
                    //return;
                }

                dest_n = 0;
                authors_c.update(destination_id_obj2, destination_store_obj2);
                dest_n += (Integer) db.getLastError().get("n");
                readers_c.update(destination_id_obj2, destination_store_obj2);
                dest_n += (Integer) db.getLastError().get("n");
                if (dest_n != 1) {
                    System.out.println("Transaction destination error in step 4");
                    //return;
                }

                //5 step
                transaction.state = Transaction.DONE;
                store_obj.put("state", transaction.state);
                //System.out.println("step5: " + store_obj);
                transactions_c.update(id_obj, store_obj);
        }
    }

    public void drop() {
        articles_c.drop();
        authors_c.drop();
        notices_c.drop();
        readers_c.drop();
        subscriptions_c.drop();
        transactions_c.drop();
    }

    public void close() {
        //close db connection
        mongo.close();
    }
}

