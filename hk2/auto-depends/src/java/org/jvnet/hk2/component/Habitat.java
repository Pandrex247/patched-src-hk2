package org.jvnet.hk2.component;

import com.sun.hk2.component.ExistingSingletonInhabitant;
import com.sun.hk2.component.ScopeInstance;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.ContractProvided;

import java.lang.annotation.Annotation;
import java.util.AbstractList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * A set of templates that constitute a world of objects.
 *
 * @author Kohsuke Kawaguchi
 * @author Jerome Dochez
 */
@SuppressWarnings("unchecked")
public class Habitat {
    /**
     * Contract type FQCN to their corresponding inhabitants.
     *
     * Can't use {@link Class} as the key so that we can create index without
     * loading contract types. Once populated upfront, it works as a read-only map.
     */
    private final MultiMap<String,NamedInhabitant> byContract = new MultiMap<String,NamedInhabitant>();

    /**
     * Index by {@link Inhabitant#type()}.
     */
    private final MultiMap<String,Inhabitant> byType = new MultiMap<String,Inhabitant>();

    public final ScopeInstance singletonScope;

    public Habitat() {

        singletonScope = new ScopeInstance("singleton", new HashMap());

        // make the habitat itself available
        add(new ExistingSingletonInhabitant<Habitat>(Habitat.class,this));
    }

    /**
     * Adds a new inhabitant.
     */
    public void add(Inhabitant i) {
        byType.add(i.typeName(),i);
    }

    /**
     * Adds a new index to look up the given inhabitant.
     *
     * @param index
     *      Primary index name, such as contract FQCN.
     * @param name
     *      Name that identifies the inhabitant among other inhabitants
     *      in the same index. Can be null for unnamed inhabitants.
     */
    public void addIndex(Inhabitant i, String index, String name) {
        byContract.add(index,new NamedInhabitant(name,i));
    }

    /**
     * Checks if the given type is a contract interface that has some implementations in this {@link Habitat}.
     *
     * <p>
     * There are two ways for a type to be marked as a contract.
     * Either it has {@link Contract}, or it's marked by {@link ContractProvided} from the implementation.
     *
     * <p>
     * Note that just having {@link Contract} is not enough to make this method return true.
     * It can still return false if the contract has no implementation in this habitat.
     *
     * <p>
     * This method is useful during the injection to determine what lookup to perform,
     * and it handles the case correctly when the type is marked as a contract by {@link ContractProvided}.
     */
    public boolean isContract(Class<?> type) {
        return byContract.containsKey(type.getName());
    }

    public boolean isContract(String fullyQualifiedClassName) {
        return byContract.containsKey(fullyQualifiedClassName);
    }

    /**
     * Gets all the habitats registered under the given {@link Contract}.
     *
     * @return
     *      can be empty but never null.
     */
    public <T> Collection<T> getAllByContract(Class<T> contractType) {
        final List<NamedInhabitant> l = byContract.get(contractType.getName());
        return new AbstractList<T>() {
            public T get(int index) {
                return (T)l.get(index).inhabitant.get();
            }

            public int size() {
                return l.size();
            }
        };
    }

    /**
     * Gets the object of the given type.
     *
     * @return
     *      can be empty but never null.
     */
    public <T> Collection<T> getAllByType(Class<T> implType) {
        final List<Inhabitant> l = byType.get(implType.getName());
        return new AbstractList<T>() {
            public T get(int index) {
                return (T)l.get(index).get();
            }

            public int size() {
                return l.size();
            }
        };
    }

    /**
     * Add an already instantiated component to this manager. The component has
     * been instantiated by external code, however dependency injection, PostConstruct
     * invocation and dependency extraction will be performed on this instance before
     * it is store in the relevant scope's resource manager.
     *
     * @param name name of the component, could be default name
     * @param component component instance
     * @throws ComponentException if the passed object is not an HK2 component or
     * injection/extraction failed.
     */
    // TODO: mutating Habitat after it's created poses synchronization issue
    public <T> void addComponent(String name, T component) throws ComponentException {
        add(new ExistingSingletonInhabitant<T>(component));
    }

    /**
     * Obtains a reference to the component inside the manager.
     *
     * <p>
     * This is the "new Foo()" equivalent in the IoC world.
     *
     * <p>
     * Depending on the {@link Scope} of the component, a new instance
     * might be created, or an existing instance might be returned.
     *
     * @return
     *      non-null.
     * @throws ComponentException
     *      If failed to obtain a requested instance.
     *      In practice, failure only happens when we try to create a
     *      new instance of the component.
     */
    public <T> T getComponent(Class<T> clazz) throws ComponentException {
        return getByType(clazz);
    }

    /**
     * Loads a component that implements the given contract and has the given name.
     *
     * @param name
     *      can be null, in which case it'll only match to the unnamed component.
     * @return
     *      null if no such servce exists.
     */
    public <T> T getComponent(Class<T> contract, String name) throws ComponentException {

        if (name!=null && name.length()==0)
            name=null;
        Inhabitant i = getInhabitant(contract, name);
        if(i!=null)
            return contract.cast(i.get());
        else
            return null;
    }    

    /**
     * Gets a lazy reference to the component.
     *
     * <p>
     * This method defers the actual instantiation of the component
     * until {@link Inhabitant#get()} is invoked.
     *
     * @return null
     *      if no such component is found.
     */
    public <T> Inhabitant<? extends T> getInhabitant(Class<T> contract, String name) throws ComponentException {
        return _getInhabitant(contract, name);
    }

    /**
     * Gets the inhabitant that has the given contract annotation and the given name.
     *
     * <p>
     * This method defers the actual instantiation of the component
     * until {@link Inhabitant#get()} is invoked.
     *
     * @return null
     *      if no such component is found.
     */
    public Inhabitant<?> getInhabitantByAnnotation(Class<? extends Annotation> contract, String name) throws ComponentException {
        return _getInhabitant(contract, name);
    }

    /**
     * Gets all the inhabitants that has the given contract.
     */
    public <T> Collection<Inhabitant<? extends T>> getInhabitants(Class<T> contract) throws ComponentException {
        final List<NamedInhabitant> l = byContract.get(contract.getName());
        return new AbstractList<Inhabitant<? extends T>>() {
            public Inhabitant<? extends T> get(int index) {
                return l.get(index).inhabitant;
            }

            public int size() {
                return l.size();
            }
        };
    }

    private Inhabitant _getInhabitant(Class contract, String name) {
        // TODO: faster implementation needed
        for (NamedInhabitant i : byContract.get(contract.getName())) {
            if(eq(i.name,name))
                return i.inhabitant;
        }
        return null;
    }

    private static boolean eq(String a, String b) {
        if(a==null && b==null)  return true;
        if(a==null || b==null)  return false;
        return a.equals(b);
    }

    /**
     * Gets the object of the given type.
     *
     * @return null if not found.
     */
    public <T> T getByType(Class<T> implType) {
        return getBy(implType, byType);
    }

    /**
     * Gets the object that has the given contract.
     *
     * <p>
     * If there are more than one of them, this method arbitrarily return
     * one of them. 
     */
    public <T> T getByContract(Class<T> contractType) {
        List<NamedInhabitant> l = byContract.get(contractType.getName());
        if(l.isEmpty())     return null;
        else                return (T)l.get(0).inhabitant.get();
    }

    private <T> T getBy(Class<T> implType, MultiMap<String, Inhabitant> index) {
        List<Inhabitant> l = index.get(implType.getName());
        if(l.isEmpty())     return null;
        else                return (T)l.get(0).get();
    }

    /**
     * Releases all the components.
     * Should be called for orderly shut-down of the system.
     * 
     * TODO: more javadoc needed
     */
    public void release() {
        // TODO: synchronization story?
        for (Entry<String, List<Inhabitant>> e : byType.entrySet())
            for (Inhabitant i : e.getValue())
                i.release();
    }
    
    private static final class NamedInhabitant {
        final String name;
        final Inhabitant inhabitant;

        public NamedInhabitant(String name, Inhabitant inhabitant) {
            this.name = name;
            this.inhabitant = inhabitant;
        }
    }
}
