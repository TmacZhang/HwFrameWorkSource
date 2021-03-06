package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Iterable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

public abstract class ASN1Set extends ASN1Primitive implements Iterable<ASN1Encodable> {
    private boolean isSorted;
    private Vector set;

    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public static ASN1Set getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1Set)) {
            return (ASN1Set) obj;
        }
        if (obj instanceof ASN1SetParser) {
            return getInstance(((ASN1SetParser) obj).toASN1Primitive());
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("failed to construct set from byte[]: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (obj instanceof ASN1Encodable) {
            ASN1Primitive primitive = ((ASN1Encodable) obj).toASN1Primitive();
            if (primitive instanceof ASN1Set) {
                return (ASN1Set) primitive;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("unknown object in getInstance: ");
        stringBuilder2.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    public static ASN1Set getInstance(ASN1TaggedObject obj, boolean explicit) {
        if (explicit) {
            if (obj.isExplicit()) {
                return (ASN1Set) obj.getObject();
            }
            throw new IllegalArgumentException("object implicit - explicit expected.");
        } else if (obj.isExplicit()) {
            if (obj instanceof BERTaggedObject) {
                return new BERSet(obj.getObject());
            }
            return new DLSet(obj.getObject());
        } else if (obj.getObject() instanceof ASN1Set) {
            return (ASN1Set) obj.getObject();
        } else {
            if (obj.getObject() instanceof ASN1Sequence) {
                ASN1Sequence s = (ASN1Sequence) obj.getObject();
                if (obj instanceof BERTaggedObject) {
                    return new BERSet(s.toArray());
                }
                return new DLSet(s.toArray());
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown object in getInstance: ");
            stringBuilder.append(obj.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    protected ASN1Set() {
        this.set = new Vector();
        this.isSorted = false;
    }

    protected ASN1Set(ASN1Encodable obj) {
        this.set = new Vector();
        this.isSorted = false;
        this.set.addElement(obj);
    }

    protected ASN1Set(ASN1EncodableVector v, boolean doSort) {
        this.set = new Vector();
        int i = 0;
        this.isSorted = false;
        while (i != v.size()) {
            this.set.addElement(v.get(i));
            i++;
        }
        if (doSort) {
            sort();
        }
    }

    protected ASN1Set(ASN1Encodable[] array, boolean doSort) {
        this.set = new Vector();
        int i = 0;
        this.isSorted = false;
        while (i != array.length) {
            this.set.addElement(array[i]);
            i++;
        }
        if (doSort) {
            sort();
        }
    }

    public Enumeration getObjects() {
        return this.set.elements();
    }

    public ASN1Encodable getObjectAt(int index) {
        return (ASN1Encodable) this.set.elementAt(index);
    }

    public int size() {
        return this.set.size();
    }

    public ASN1Encodable[] toArray() {
        ASN1Encodable[] values = new ASN1Encodable[size()];
        for (int i = 0; i != size(); i++) {
            values[i] = getObjectAt(i);
        }
        return values;
    }

    public ASN1SetParser parser() {
        return new ASN1SetParser() {
            private int index;
            private final int max = ASN1Set.this.size();

            public ASN1Encodable readObject() throws IOException {
                if (this.index == this.max) {
                    return null;
                }
                ASN1Encodable obj = ASN1Set.this;
                int i = this.index;
                this.index = i + 1;
                obj = obj.getObjectAt(i);
                if (obj instanceof ASN1Sequence) {
                    return ((ASN1Sequence) obj).parser();
                }
                if (obj instanceof ASN1Set) {
                    return ((ASN1Set) obj).parser();
                }
                return obj;
            }

            public ASN1Primitive getLoadedObject() {
                return this;
            }

            public ASN1Primitive toASN1Primitive() {
                return this;
            }
        };
    }

    public int hashCode() {
        Enumeration e = getObjects();
        int hashCode = size();
        while (e.hasMoreElements()) {
            hashCode = (hashCode * 17) ^ getNext(e).hashCode();
        }
        return hashCode;
    }

    ASN1Primitive toDERObject() {
        if (this.isSorted) {
            ASN1Set derSet = new DERSet();
            derSet.set = this.set;
            return derSet;
        }
        Vector v = new Vector();
        for (int i = 0; i != this.set.size(); i++) {
            v.addElement(this.set.elementAt(i));
        }
        ASN1Set derSet2 = new DERSet();
        derSet2.set = v;
        derSet2.sort();
        return derSet2;
    }

    ASN1Primitive toDLObject() {
        ASN1Set derSet = new DLSet();
        derSet.set = this.set;
        return derSet;
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof ASN1Set)) {
            return false;
        }
        ASN1Set other = (ASN1Set) o;
        if (size() != other.size()) {
            return false;
        }
        Enumeration s1 = getObjects();
        Enumeration s2 = other.getObjects();
        while (s1.hasMoreElements()) {
            ASN1Encodable obj1 = getNext(s1);
            ASN1Encodable obj2 = getNext(s2);
            ASN1Primitive o1 = obj1.toASN1Primitive();
            ASN1Primitive o2 = obj2.toASN1Primitive();
            if (o1 != o2) {
                if (!o1.equals(o2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private ASN1Encodable getNext(Enumeration e) {
        ASN1Encodable encObj = (ASN1Encodable) e.nextElement();
        if (encObj == null) {
            return DERNull.INSTANCE;
        }
        return encObj;
    }

    private boolean lessThanOrEqual(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        boolean z = false;
        for (int i = 0; i != len; i++) {
            if (a[i] != b[i]) {
                if ((a[i] & 255) < (b[i] & 255)) {
                    z = true;
                }
                return z;
            }
        }
        if (len == a.length) {
            z = true;
        }
        return z;
    }

    private byte[] getDEREncoded(ASN1Encodable obj) {
        try {
            return obj.toASN1Primitive().getEncoded(ASN1Encoding.DER);
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot encode object added to SET");
        }
    }

    protected void sort() {
        if (!this.isSorted) {
            this.isSorted = true;
            if (this.set.size() > 1) {
                boolean swapped = true;
                int lastSwap = this.set.size() - 1;
                while (true) {
                    int lastSwap2 = lastSwap;
                    if (swapped) {
                        int swapIndex = 0;
                        byte[] a = getDEREncoded((ASN1Encodable) this.set.elementAt(0));
                        swapped = false;
                        for (lastSwap = 0; lastSwap != lastSwap2; lastSwap++) {
                            byte[] b = getDEREncoded((ASN1Encodable) this.set.elementAt(lastSwap + 1));
                            if (lessThanOrEqual(a, b)) {
                                a = b;
                            } else {
                                Object o = this.set.elementAt(lastSwap);
                                this.set.setElementAt(this.set.elementAt(lastSwap + 1), lastSwap);
                                this.set.setElementAt(o, lastSwap + 1);
                                swapped = true;
                                swapIndex = lastSwap;
                            }
                        }
                        lastSwap = swapIndex;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    boolean isConstructed() {
        return true;
    }

    public String toString() {
        return this.set.toString();
    }

    public Iterator<ASN1Encodable> iterator() {
        return new Arrays.Iterator(toArray());
    }
}
