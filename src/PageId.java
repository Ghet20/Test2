public class PageId implements IPageId {

    private int numPage;

    //Constructeur crée un  pageId avec son numéro de page, un new pageId(1) donnera la page 1)
    public PageId (int numPage) {
        this.numPage = numPage;
    }

    //renvoie le n° de page
    public int getNumPage() {
        return numPage;
    }

    
    //compare deux pageId pour savoir s'ils sont à la mm page 
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PageId)) {
            return false;
        }
        PageId autre = (PageId) obj;
        return this.numPage == autre.numPage;
    }
}
