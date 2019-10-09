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


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

class MovieRecommender {
    private int totalReviews;
    private UserBasedRecommender recommender;
    private HashMap<String, Long> hUserId = new HashMap<>();
    private HashMap<String, Long> hProductId = new HashMap<>();
    private String[] tmpArray;
    private String csvFile = "dataset.csv";

    MovieRecommender(String path) throws IOException, TasteException {
        processFile(path);
        handleMahout();
    }

    private void processFile(String path) throws IOException {
        int flag = 0;
        long userId = 0;
        long productId = 0;
        InputStream fileStream = new FileInputStream(path);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.US_ASCII);
        FileWriter fileWriter = new FileWriter(csvFile);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        BufferedReader bufferedReader = new BufferedReader(decoder);
        String content;

        String line = "";
        while ((content = bufferedReader.readLine()) != null) {

            if (content.startsWith("product/productId:") && flag == 0) {

                String tmpProduct;
                long tmpProductId;
                tmpProduct = content.split(" ")[1];
                if (hProductId.containsKey(tmpProduct)) {
                    tmpProductId = hProductId.get(tmpProduct);

                } else {
                    tmpProductId = productId;
                    hProductId.put(tmpProduct, productId++);
                }
                line = tmpProductId + ",";
                flag = 1;

            } else if (content.startsWith("review/userId:") && flag == 1) {
                String tmpUser = content.split(" ")[1];
                long tmpUserId;
                if (hUserId.containsKey(tmpUser)) {
                    tmpUserId = hUserId.get(tmpUser);

                } else {
                    tmpUserId = userId;
                    hUserId.put(tmpUser, userId++);
                }
                flag = 2;
                line = tmpUserId + "," + line;

            } else if (content.startsWith("review/score:") && flag == 2) {
                String tmpScore = content.split(" ")[1];
                totalReviews++;
                flag = 0;
                line = line + tmpScore + "\n";
                bufferedWriter.write(line);
            }
        }

        bufferedWriter.flush();
        fileWriter.close();
    }

    private void handleMahout() throws TasteException, IOException {
        tmpArray = new String[hProductId.size()];
        for (Map.Entry<String, Long> mEntry : hProductId.entrySet()) {
            tmpArray[mEntry.getValue().intValue()] = mEntry.getKey();
        }

        DataModel model = new FileDataModel(new File(csvFile));
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
        this.recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
    }

    int getTotalReviews() {
        return totalReviews;
    }

    int getTotalProducts() {
        return hProductId.size();
    }

    int getTotalUsers() {
        return hUserId.size();
    }

    List<String> getRecommendationsForUser(String user) throws TasteException {
        List<String> recommendations = new ArrayList<>();
        for (RecommendedItem recommendedItem : recommender.recommend(hUserId.get(user), 3)) {
            recommendations.add(tmpArray[(int) recommendedItem.getItemID()]);
        }
        return recommendations;

    }

}
