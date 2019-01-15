package purejava;

import org.apache.commons.lang.time.StopWatch;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import purejava.lock.ClusteredDatastoreLock;

public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);
  private static final int NB_CLUSTERED_LOCK = 5000;

  public static void main(String[] args) throws Exception {

    LOG.info("-- début du test");
    EmbeddedCacheManager manager = new DefaultCacheManager("moose-cluster.xml");
    //
    ClusteredLockManager clusteredLockManager = EmbeddedClusteredLockManagerFactory.from(manager);

    StopWatch t = new StopWatch();
    for (int i = 1; i <= 3; i++) {

      t.start();
      LOG.info("-- début du test[{}]:", i);
      LOG.info("-- création de {} locks", NB_CLUSTERED_LOCK);
      createLockUnlockThenRemoveLock(clusteredLockManager);
      LOG.info("-- fin du test[{}]", i);
      t.stop();
      final long elapsedTimeInSeconde = t.getTime() / 1000;
      LOG.info("-- test[{}] => Temps écoulé: [{}] secondes", i, elapsedTimeInSeconde);
      t.reset();
    }

    LOG.info("-- verification que tous les locks n'existe plus:");
    checkLocksAreRemoved(clusteredLockManager);
    LOG.info("-- fin du test");
    LOG.info("-- A ce stade vérifier qu'il n'y a plus plus d'instance de ClusteredLockImpl dans la JVM.");

    while (true) {
      //boucle infinie :)
    }
  }

  private static void checkLocksAreRemoved(final ClusteredLockManager clusteredLockManager) {
    for (int i = 0; i < NB_CLUSTERED_LOCK; i++) {
      try {
        String lock = "lock_" + i;
        final boolean isDefined = clusteredLockManager.isDefined(lock);
        if (isDefined) {

          LOG.error("Le verrou [{}] est toujours défini!", lock);
        }
        final ClusteredLock clusteredLock = clusteredLockManager.get(lock);
        if (clusteredLock != null) {

          LOG.error("Le verrou [{}] n'a pas été supprimé!", lock);
        }
      }
      catch (Exception e) {
        LOG.error("Error:[{}]", e.getMessage());
      }
    }
  }

  private static void createLockUnlockThenRemoveLock(final ClusteredLockManager clusteredLockManager) {
    for (int i = 0; i < NB_CLUSTERED_LOCK; i++) {

      String lock = "lock_" + i;
      ClusteredDatastoreLock clusteredDatastoreLock = new ClusteredDatastoreLock(clusteredLockManager);
      clusteredDatastoreLock.defineLock(lock);
      clusteredDatastoreLock.tryLock(lock, 1000);
      clusteredDatastoreLock.unlock(lock);
    }
    for (int i = 0; i < NB_CLUSTERED_LOCK; i++) {

      String lock = "lock_" + i;
      ClusteredDatastoreLock clusteredDatastoreLock = new ClusteredDatastoreLock(clusteredLockManager);
      clusteredDatastoreLock.tryLock(lock, 1000);
      clusteredDatastoreLock.unlock(lock);
      clusteredDatastoreLock.removeLock(lock);
    }
  }

}
