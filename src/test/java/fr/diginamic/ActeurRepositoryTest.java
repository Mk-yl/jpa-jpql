package fr.diginamic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

public class ActeurRepositoryTest {

	private static EntityManagerFactory emf;
	private EntityManager em;

	/**
	 * Extraire tous les acteurs triés dans l'ordre alphabétique des identités
	 */
	@Test
	public void testExtraireActeursTriesParIdentite() {
		TypedQuery<Acteur> query = em.createQuery("SELECT a FROM Acteur a ORDER BY a.identite ASC", Acteur.class);
		List<Acteur> acteurs = query.getResultList();

		assertEquals(1137, acteurs.size());
		assertEquals("A.J. Danna", acteurs.get(0).getIdentite());
	}

	/**
	 * Extraire l'actrice appelée Marion Cotillard
	 */
	@Test
	public void testExtraireActeursParIdentite() {
		// Utilisation d'un paramètre nommé pour sécuriser la requête
		TypedQuery<Acteur> query = em.createQuery("SELECT a FROM Acteur a WHERE a.identite = :acteurIdentite", Acteur.class);
		query.setParameter("acteurIdentite", "Marion Cotillard");

		List<Acteur> acteurs = query.getResultList();

		assertEquals(1, acteurs.size());
		assertEquals("Marion Cotillard", acteurs.get(0).getIdentite());
	}

	/**
	 * Extraire la liste des acteurs dont l'année de naissance est 1985. Astuce: fonction year(...)
	 */
	@Test
	public void testExtraireActeursParAnneeNaissance() {
		TypedQuery<Acteur> query = em.createQuery("SELECT a FROM Acteur a WHERE year(a.anniversaire) = :anneeNaissance", Acteur.class);
		query.setParameter("anneeNaissance", 1985);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(10, acteurs.size());
	}

	/**
	 * Extraire la liste des actrices ayant joué le rôle d'Harley Quinn
	 */
	@Test
	public void testExtraireActeursParRole() {
		// Paramètre pour le nom du rôle
		TypedQuery<Acteur> query = em.createQuery("SELECT a FROM Acteur a JOIN a.roles r WHERE r.nom = :roleName", Acteur.class);
		query.setParameter("roleName", "Harley Quinn");

		List<Acteur> acteurs = query.getResultList();

		assertEquals(1, acteurs.size());
		assertEquals("Margot Robbie", acteurs.get(0).getIdentite());
	}

	/**
	 * Extraire la liste de tous les acteurs ayant joué dans un film paru en 2015.
	 */
	@Test
	public void testExtraireActeursParFilmParuAnnee() {
		// Paramètre pour l'année du film
		TypedQuery<Acteur> query = em.createQuery("SELECT a FROM Acteur a JOIN a.roles r JOIN r.film f WHERE f.annee = :filmAnnee", Acteur.class);
		query.setParameter("filmAnnee", 2015);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(119, acteurs.size());
	}

	/**
	 * Extraire la liste de tous les acteurs ayant joué dans un film dont le pays d'origine est France
	 */
	@Test
	public void testExtraireActeursParPays() {
		// Paramètre pour le nom du pays
		TypedQuery<Acteur> query = em.createQuery("SELECT a FROM Acteur a JOIN a.roles r JOIN r.film f JOIN f.pays p WHERE p.nom = :pays", Acteur.class);
		query.setParameter("pays", "France");

		List<Acteur> acteurs = query.getResultList();

		assertEquals(158, acteurs.size());
	}

	/**
	 * Extraire la liste de tous les acteurs ayant joué dans un film paru en 2017 et dont le pays d'origine est France
	 */
	@Test
	public void testExtraireActeursParListePaysEtAnnee() {
		// Paramètres pour l'année du film et le pays
		TypedQuery<Acteur> query = em.createQuery(
				"SELECT a FROM Acteur a JOIN a.roles r JOIN r.film f JOIN f.pays p WHERE f.annee = :filmAnnee AND p.nom = :pays", Acteur.class);
		query.setParameter("filmAnnee", 2017);
		query.setParameter("pays", "France");

		List<Acteur> acteurs = query.getResultList();

		assertEquals(24, acteurs.size());
	}

	/**
	 * Extraire la liste de tous les acteurs ayant joué dans un film réalisé par Ridley Scott entre les années 2010 et 2020
	 */
	@Test
	public void testExtraireParRealisateurEntreAnnee() {
		// Paramètres pour le réalisateur et les années
		TypedQuery<Acteur> query = em.createQuery(
				"SELECT a FROM Acteur a JOIN a.roles r JOIN r.film f JOIN f.realisateurs real WHERE real.identite = :realisateurIdentite AND f.annee BETWEEN :startYear AND :endYear",
				Acteur.class);
		query.setParameter("realisateurIdentite", "Ridley Scott");
		query.setParameter("startYear", 2010);
		query.setParameter("endYear", 2020);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(27, acteurs.size());
	}

	/**
	 * Extraire la liste de tous les réalisateurs ayant réalisé un film dans lequel Brad Pitt a joué
	 */
	@Test
	public void testExtraireRealisateursParActeur() {
		// Paramètre pour l'acteur
		TypedQuery<Realisateur> query = em.createQuery(
				"SELECT DISTINCT r FROM Realisateur r JOIN r.films f JOIN f.roles ro WHERE ro.acteur.identite = :acteurIdentite",
				Realisateur.class);
		query.setParameter("acteurIdentite", "Brad Pitt");

		List<Realisateur> realisateurs = query.getResultList();

		assertEquals(6, realisateurs.size());
	}

	@BeforeEach
	public void ouvertureEm() {
		em = emf.createEntityManager();
	}

	@AfterEach
	public void fermetureEm() {
		em.close();
	}

	@BeforeAll
	public static void initDatabase() {
		emf = Persistence.createEntityManagerFactory("movie_db");
		EntityManager em = emf.createEntityManager();

		try {
			if (em.createQuery("FROM Acteur").getResultList().size() == 0) {
				em.getTransaction().begin();
				Path home = Paths.get(ActeurRepositoryTest.class.getClassLoader().getResource("data.sql").toURI());
				String[] queries = Files.readAllLines(home).stream().collect(Collectors.joining("\n")).split(";");
				for (String query : queries) {
					em.createNativeQuery(query).executeUpdate();
				}
				em.getTransaction().commit();
			}
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
		em.close();
	}

	@AfterAll
	public static void fermetureRessources() {
		emf.close();
	}
}
