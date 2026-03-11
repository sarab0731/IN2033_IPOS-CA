package integration.interfaces;
import domain.CatalogueItem;

import java.util.List;

public interface IInventoryService {

	List<CatalogueItem> getCatalogue();

	/**
	 * 
	 * @param itemID
	 */
	String deductStock(String itemID);

}