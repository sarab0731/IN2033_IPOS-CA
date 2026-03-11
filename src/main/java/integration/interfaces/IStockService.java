package integration.interfaces;

import domain.CatalogueItem;

import java.util.List;

/**
 * So that user of PU can search for a specific item by its item ID and get all available information (description, package type, units in a pack, cost, availability)
 */
public interface IStockService {

	/**
	 * So that user of PU can search for a specific item by its item ID and get all available information (description, package type, units in a pack, cost, availability). Returns an object of type catalogueItem.
	 * @param itemID
	 */
	CatalogueItem itemAvailability(String itemID);

	/**
	 * So that user of PU can search stock based on a keyword, returns a List containing objects of type catalogueItem.
	 * User will be able to see item ID, description, package type, units in a pack, cost.
	 * @param keyword
	 */
	List<CatalogueItem> searchStock(String keyword);


	/**
	 * Confirms reserved items as sold, removing reserved items from stock on CA
	 * @param itemID
	 */
	String deductStock(String itemID);


	/**
	 * Returns a List of CatalogueItems which contains all information on all catalogue items
	 */
	List getCatalogue();

}