
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Scanner;


public class DiskManager implements IDiskManager {

    private String dmDir;
    private int pageSize;
    private int nbPages;
    private ArrayList<Integer> pagesLibres;

    public DiskManager() {
        dmDir = null;
        pageSize = 0;
        nbPages = 0;
        pagesLibres = new ArrayList<Integer>();
    }
    
    
    //rattache le DiskManager a un dossier, avec une taille de page.
    
    @Override
    public void Init(String dmDir, int pageSize) throws DiskManagerException {
    	if (this.dmDir != null) {
            throw new DiskManagerException("Init a deja ete appele");
        }
        if (dmDir == null) {
            throw new DiskManagerException("Dossier null");
        }
        if (pageSize <= 0) {
            throw new DiskManagerException("Taille de page invalide : " + pageSize);
        }

        File dossier = new File(dmDir);
        if (!dossier.exists()) {
        dossier.mkdirs();
        }
        if (!dossier.isDirectory()) {
        	throw new DiskManagerException("Impossible d'utiliser le dossier " + dmDir);
        }
        
        // Cas dossier vide
        File save = new File(dmDir, "dm.save");
        if (!save.exists()) {
        	this.dmDir = dmDir;
        	this.pageSize = pageSize;
            return;
        }
        

        // Cas dossier utilisé : on relit dm.save
        int tailleLue = 0;
        int nbPagesLu = 0;
        ArrayList<Integer> libresLues = new ArrayList<Integer>();
        try {
            Scanner sc = new Scanner(save);
            tailleLue = sc.nextInt();
            nbPagesLu = sc.nextInt();
            while (sc.hasNextInt()) {
            	libresLues.add(sc.nextInt());
            }
            sc.close();
           } catch (Exception e) {
            throw new DiskManagerException("Erreur de lecture de dm.save");
          }

            if (tailleLue != pageSize) {
            	 throw new DiskManagerException("Taille de page incompatible");
            }
            this.dmDir = dmDir;
            this.pageSize = pageSize;
            this.nbPages = nbPagesLu;
            this.pagesLibres = libresLues;
        
       
    }
    
    //vérifie si init fait
    private void verifierInit() throws DiskManagerException {
        if (dmDir == null) {
            throw new DiskManagerException("Init n'a pas ete appele");
        }
        
    }
    
    //vérifie si la page est utilisable
    private int verifierPage(IPageId ipid) throws DiskManagerException {
        if (ipid == null) {
            throw new DiskManagerException("PageId null");
        }
        if (!(ipid instanceof PageId)) {
            throw new DiskManagerException("Ce n'est pas un PageId");
        }
        int num = ((PageId) ipid).getNumPage();
        if (num < 0 || num >= nbPages) {
            throw new DiskManagerException("La page " + num + " n'existe pas");
        }
        if (pagesLibres.contains(num)) {
            throw new DiskManagerException("La page " + num + " n'est pas allouee");
        }
        return num;
    }

    private void verifierBuffer(ByteBuffer buffer) throws DiskManagerException {
        if (buffer == null) {
            throw new DiskManagerException("Buffer null");
        }
        if (buffer.limit() < pageSize) {
            throw new DiskManagerException("Buffer trop petit pour une page");
        }
    }

    //Sauvegarde les infos du DM
    @Override
    public void Save() throws DiskManagerException {
    	verifierInit();
        try {
            PrintWriter pw = new PrintWriter(new File(dmDir, "dm.save"));
            pw.println(pageSize);
            pw.println(nbPages);
            for (int i = 0; i < pagesLibres.size(); i++) {
                pw.print(pagesLibres.get(i) + " ");
            }
            pw.println();
            pw.close();
        } catch (IOException e) {
            throw new DiskManagerException("Erreur d'ecriture de dm.save");
        }
    }

    // AllocPage : reserve une page et renvoie son PageId
    @Override
    public IPageId AllocPage() throws DiskManagerException {
    	verifierInit();
        
    	// Cas 1 : on reutilise une page libre
        if (pagesLibres.size() > 0) {
            int num = pagesLibres.remove(0);
            return new PageId(num);
        }

        // Cas 2 : on ajoute une page a la fin du fichier
        int num = nbPages;
        try {
            RandomAccessFile fichier = new RandomAccessFile(new File(dmDir, "data.bin"), "rw");
            fichier.setLength((num + 1) * pageSize);
            fichier.close();
        } catch (IOException e) {
            throw new DiskManagerException("Erreur lors de l'ajout d'une page");
        }
        nbPages = nbPages + 1;
        return new PageId(num);
    }

    //copie le contenu d'une page du disque dans le buffer

    @Override
    public void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
    	verifierInit();
    	int num = verifierPage(ipid);
    	verifierBuffer(buffer);
        byte[] tab = new byte[pageSize];

        try {
            RandomAccessFile fichier = new RandomAccessFile(new File(dmDir, "data.bin"), "r");
            fichier.seek(num * pageSize);
            fichier.read(tab);
            fichier.close();
        } catch (IOException e) {
            throw new DiskManagerException("Erreur de lecture de la page " + num);
        }

        for (int i = 0; i < pageSize; i++) {
            buffer.put(i, tab[i]);
        }
    }

    // inverse de ReadPage, on copie le buffer dans la page sur le disque
    @Override
    public void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
    	verifierInit();
    	int num = verifierPage(ipid);
    	verifierBuffer(buffer);
    	
        byte[] tab = new byte[pageSize];

        for (int i = 0; i < pageSize; i++) {
            tab[i] = buffer.get(i);
        }

        try {
            RandomAccessFile fichier = new RandomAccessFile(new File(dmDir, "data.bin"), "rw");
            fichier.seek(num * pageSize);
            fichier.write(tab);
            fichier.close();
        } catch (IOException e) {
            throw new DiskManagerException("Erreur d'ecriture de la page " + num);
        }
    }
    //libérer une page
    @Override
    public void DeallocPage(IPageId ipid) throws DiskManagerException {
    	verifierInit();
    	int num = verifierPage(ipid);
        pagesLibres.add(num);
    }
    
    //indique cb de pages sont utilisées
    @Override
    public int GetActivePageCount() {
        return nbPages - pagesLibres.size();
    }
    
    //retourne la taille d'une page
    @Override
    public int GetPageSize() {
        return pageSize;
    }
}
