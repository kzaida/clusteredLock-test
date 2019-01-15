package purejava.lock;

import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.lock.exception.ClusteredLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Implémentation utilisant des verrous distribués défini par l'API {@link ClusteredLock}.
 */
public class ClusteredDatastoreLock implements DatastoreLock {

  private static final Logger LOG = LoggerFactory.getLogger(ClusteredDatastoreLock.class);
  private static final int LOCK_MAX_ATTEMPT = 5;
  private static final int FREELOCK_MAX_ATTEMPT = 2;
  private Integer maxDelaySleepMillisecondes;
  private ClusteredLockManager clm;

  public ClusteredDatastoreLock(ClusteredLockManager clm) {
    this.clm = clm;
    this.maxDelaySleepMillisecondes = 1000;
  }

  @Override
  public boolean defineLock(final String lockName) {
    LOG.debug("Création d'un verrou ayant pour nom [{}].", lockName);
    return this.clm.defineLock(lockName);
  }

  @Override
  public boolean removeLock(final String lockName) {

    boolean isLockedRemoved = false;
    LOG.debug("Demande de suppression du verrou [{}].", lockName);
    try {
      isLockedRemoved = this.clm.remove(lockName).get();
      if (!isLockedRemoved) {
        LOG.warn("Le verrou [{}] n'a pas été supprimé.", lockName);
      }
    }
    catch (Exception e) {
      LOG.error("Impossible de libérer le verrou [{}] dans le ClusterLockManager", lockName, e);
    }
    return isLockedRemoved;
  }


  @Override
  public void tryLock(final String lockName, final long timeInMilliseconds) {

    LOG.debug("Demande d'acquisition du verrou identifié par [{}]", lockName);
    int n = LOCK_MAX_ATTEMPT;
    ClusteredLock clusteredLock;
    try {
      clusteredLock = this.clm.get(lockName);
    }
    catch (ClusteredLockException e) {
      String msg = String.format("le verrou identifié par %s n'est pas défini dans le ClusteredLockManager.", lockName);
      LOG.error(msg, e);
      return;
    }

    do {
      try {
        Boolean get = clusteredLock.tryLock(timeInMilliseconds, TimeUnit.MILLISECONDS).get();
        if (get.booleanValue()) {

          LOG.debug("Verrou [{}] acquis ms", lockName);
          return;
        } else {
          n--;
          LOG.warn("Timeout acquisition du verrou [{}], [{}] tentatives restantes", lockName, n);
          enterRandomSleepMode();
          clusteredLock = this.clm.get(lockName);
        }
      }
      catch (ClusteredLockException | ExecutionException e) {
        n--;
        LOG.error("Impossible acquerir le verrou [{}], [{}] tentatives restantes", lockName, n, e);
      }
      catch (InterruptedException e) {
        n--;
        LOG.error("Impossible acquerir le verrou [{}], [{}] tentatives restantes", lockName, n, e);
        Thread.currentThread().interrupt();
      }
    }
    while (n > 0);

    String msg = String.format("Echec acquisition du verrou [%s]. %s tentatives effectuées",
            lockName, LOCK_MAX_ATTEMPT);
    LOG.error(msg);
  }

  @Override
  public boolean unlock(String lockName) {

    LOG.debug("Demande de libération du verrou identifié par [{}].", lockName);
    ClusteredLock clusteredLock;
    try {
      clusteredLock = this.clm.get(lockName);
    }
    catch (ClusteredLockException e) {
      LOG.error("Le verrou [{}] n'existe pas.", lockName, e);
      return false;
    }

    int n = FREELOCK_MAX_ATTEMPT;
    do {
      try {

        clusteredLock.unlock().get();
        LOG.debug("Verrou identifié par [{}] libérée.", lockName);
        return true;
      }
      catch (ClusteredLockException | ExecutionException e) {
        n--;
        LOG.warn("Le verrou {} ne peut etre libere. Reste {} tentatives", lockName, n, e);
      }
      catch (InterruptedException e) {
        n--;
        LOG.warn("Le verrou {} ne peut etre libere. Reste {} tentatives", lockName, n, e);
        Thread.currentThread().interrupt();
      }
    }
    while (n > 0);
    LOG.error("Impossible de libérer le verrou {}. Abondon", lockName);
    return false;
  }

  protected void enterRandomSleepMode() throws InterruptedException {

    long waitAmount = ThreadLocalRandom.current().nextLong(maxDelaySleepMillisecondes);
    LOG.debug("Timeout evitement: Passage en sleep mode pour une durée de {} ms", waitAmount);
    Thread.sleep(waitAmount);
  }

}
