/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2013-2016, Kenneth Leung. All rights reserved. */

package czlab.twisty;


/**
 *
 * @author Kenneth Leung
 *
 */
public interface IPassword {

  /**
   * true if the hash matches the internal value.
   */
  public boolean validateHash(String targetHashed);

  /**
   * Get the password.
   */
  public char[] toCharArray();

  /**
   * A map(2) {:hashed 'hashed value' :salt 'salt'}
   */
  public Object stronglyHashed();

  /**
   * A map(2) {:hashed 'hashed value' :salt 'salt'}
   */
  public Object hashed();

  /**
   * The encoded value.
   */
  public String encoded();

  /**
   * The text value.
   */
  public String text();

}

