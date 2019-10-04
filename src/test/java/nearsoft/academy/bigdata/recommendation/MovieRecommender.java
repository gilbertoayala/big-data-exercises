package nearsoft.academy.bigdata.recommendation;


import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class MovieRecommender {
    private String pathToFile;
    private int totalReviews;
    private int totalProducts;
    private int totalUsers;
    private UserBasedRecommender recommender;
    public  HashMap<String, Long> hUserId = new HashMap<>();
    public HashMap<String, Long> hProductId = new HashMap<>();
    String [] tmpArray;
    public MovieRecommender(String path) throws IOException, TasteException {


        loadFile(path);
    }

    public void loadFile(String path) throws IOException, TasteException {

        int flag = 0;
        long userId = 0;
        long productId = 0;

        String pathToFile = path;
        InputStream fileStream = new FileInputStream(pathToFile);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "US-ASCII");
        FileWriter fileWriter = new FileWriter("dataset.csv");
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        BufferedReader bufferedReader = new BufferedReader(decoder);
        String content;

        String line = "";
        while ((content = bufferedReader.readLine()) != null) {
            if (content.startsWith("product/productId:") && flag == 0) {

                String tmpProduct;
                long tmpProductId;
                tmpProduct = content.split(" ")[1];
                if(hProductId.containsKey(tmpProduct)){
                    tmpProductId = hProductId.get(tmpProduct);

                }else{
                    tmpProductId = productId;
                    hProductId.put(tmpProduct,productId++);
                }
//                hProductId.put(tmpProduct, productId);
//                productId++;
                line = tmpProductId + ",";
                flag = 1;
            } else if (content.startsWith("review/userId:") && flag == 1) {
                String tmpUser = content.split(" ")[1];
                long tmpUserId;
                if(hUserId.containsKey(tmpUser)){
                    tmpUserId = hUserId.get(tmpUser);
                }else{
                    tmpUserId = userId;
                    hUserId.put(tmpUser,userId++);
                }
//                hUserId.put(tmpUser, userId);
//                userId++;
                flag = 2;
                line = tmpUserId + "," + line;

            } else if (content.startsWith("review/score:") && flag == 2) {
                String tmpScore= content.split(" ")[1];
                totalReviews++;
                flag = 0;
                line = line +tmpScore+"\n";
                bufferedWriter.write(line);
            }
        }
        bufferedWriter.flush();


        tmpArray = new String[hProductId.size()];
        for(Map.Entry<String, Long> me : hProductId.entrySet()){
            tmpArray[me.getValue().intValue()] = me.getKey();
        }
        DataModel model = new FileDataModel(new File("dataset.csv"));
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
        this.recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
    }


    public int getTotalReviews() {
        // every time it reads productId, userId, score add 1 to total reviews


        return totalReviews;
    }

    public int getTotalProducts() {
        totalProducts =hProductId.size();
        return totalProducts;
    }

    public int getTotalUsers() {
        // read users and add them to hash table if it is in the hashtable dont and if hes added then update total user

        totalUsers = hUserId.size();
        return totalUsers;
    }


    public List<String> getRecommendationsForUser(String user) throws TasteException {
        List<String> recommendations = new ArrayList<>();
        for(RecommendedItem ri : recommender.recommend(hUserId.get(user), 5)){
            recommendations.add(tmpArray[(int)ri.getItemID()]);
        }
        return recommendations;

    }

}
