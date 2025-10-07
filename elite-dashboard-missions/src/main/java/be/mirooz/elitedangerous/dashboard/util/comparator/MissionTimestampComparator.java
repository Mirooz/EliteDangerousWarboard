package be.mirooz.elitedangerous.dashboard.util.comparator;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;

import java.util.Comparator;

public class MissionTimestampComparator implements Comparator<Mission> {
    private boolean desc;
    private boolean progress = false;

    public MissionTimestampComparator(boolean desc, boolean progress) {
        this.desc = desc;
        this.progress = progress;
    }

    public MissionTimestampComparator(boolean desc) {
        this.desc = desc;
    }

    @Override
    public int compare(Mission m1, Mission m2) {
        if (progress) {
            double ratio1 = (m1.getTargetCount() > 0) ? (double) m1.getCurrentCount() / m1.getTargetCount() : 0.0;
            double ratio2 = (m2.getTargetCount() > 0) ? (double) m2.getCurrentCount() / m2.getTargetCount() : 0.0;

            // Tri décroissant sur le ratio
            int cmp = Double.compare(ratio2, ratio1);
            if (cmp != 0) return cmp;
        }

        // Fallback : trier par date d'acceptation (plus ancienne en premier)
        if (m1.getAcceptedTime() == null && m2.getAcceptedTime() == null) return 0;
        if (m1.getAcceptedTime() == null) return 1;
        if (m2.getAcceptedTime() == null) return -1;
        if (desc) {
            return m1.getAcceptedTime().compareTo(m2.getAcceptedTime());
        }
        return m2.getAcceptedTime().compareTo(m1.getAcceptedTime());
    }
}