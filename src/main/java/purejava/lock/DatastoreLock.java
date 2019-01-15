package purejava.lock;

/**
 * Interface permettant d'intéragir sur des verrous.
 */
public interface DatastoreLock {

  /**
   * Créer un verrou avec le nom passé en paramètre.
   *
   * @param name Le nom du verrou
   *
   * @return true si le verrou a été créé, false sinon.
   */
  boolean defineLock(String name);

  /**
   * Supprime un verrou précédemment créé.
   *
   * @param name Le nom du verrou
   *
   * @return true si le verrou a été supprimé, false sinon.
   */
  boolean removeLock(String name);

  /**
   * Tente d'acquérir le verrou dans le temps maximum spécifié en paramètre.
   *
   * @param name               Le nom du verrou
   * @param timeInMilliseconds Temps d'attente maximum en milliseconde
   */
  void tryLock(String name, long timeInMilliseconds);

  /**
   * Relache le verrou.
   *
   * @param name Le nom du verrou
   *
   * @return true si le verrou été relaché.
   */
  boolean unlock(String name);

}
