//
// This file was generated by the JPA Modeler
//
/* MIT License
* 
* Copyright (c) 2021 Braully Rocha
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/

package com.github.braully.domain;

import com.github.braully.constant.Attr;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "financial")
@DiscriminatorValue("0")
@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER, name = "type_id",
        columnDefinition = "smallint default '0'", length = 1)
@Getter
@Setter
public class Account extends AbstractMigrableEntity implements Serializable, ISystemEntity {

    @Basic
    protected String name;

    @Basic
    protected String description;

    @ManyToOne(targetEntity = Account.class)
    protected Account parentAccount;

    @Attr("hidden")
    @Column(name = "system_lock",
            columnDefinition = "boolean default false")
    protected Boolean systemLock;

    @Basic
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "char(1)", length = 1)
    protected AccountType typeGL;

    @Column(name = "grouping_gl")
    @Basic
    protected String groupingGL;

    public Account() {

    }

    @Override
    public String toString() {
        return name + " (" + this.id + ")";
    }

    public boolean isCredit() {
        return this.typeGL == AccountType.C;
    }

    public boolean isDebit() {
        return this.typeGL == AccountType.D;
    }

    public static enum AccountType {
        C("Credit"), D("Debit");
        String type;

        private AccountType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }
}
